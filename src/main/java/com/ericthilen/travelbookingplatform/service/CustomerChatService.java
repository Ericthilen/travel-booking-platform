package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.dto.CustomerChatIdentificationRequest;
import com.ericthilen.travelbookingplatform.model.CustomerChatConversation;
import com.ericthilen.travelbookingplatform.model.CustomerChatMessage;
import com.ericthilen.travelbookingplatform.model.CustomerChatSender;
import com.ericthilen.travelbookingplatform.model.CustomerChatStatus;
import com.ericthilen.travelbookingplatform.model.CustomerChatTicket;
import com.ericthilen.travelbookingplatform.repository.BookingRepository;
import com.ericthilen.travelbookingplatform.repository.CustomerChatConversationRepository;
import com.ericthilen.travelbookingplatform.repository.CustomerChatMessageRepository;
import com.ericthilen.travelbookingplatform.repository.CustomerChatTicketRepository;
import com.ericthilen.travelbookingplatform.repository.EmailTicketRepository;
import com.ericthilen.travelbookingplatform.repository.TravelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CustomerChatService {

    private static final int ARCHIVE_AFTER_MINUTES = 15;
    private static final Pattern BOOKING_NUMBER_PATTERN =
            Pattern.compile("\\bE?\\d{6,10}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOOKING_NUMBER_HINT_PATTERN =
            Pattern.compile(
                    "\\bboknings\\s*(nummer|nr)?\\b|\\bbokningsnr\\b",
                    Pattern.CASE_INSENSITIVE
            );
    private static final String BOOKING_NOT_FOUND_MESSAGE =
            "Vi kunde tyvärr inte hitta din bokning på det numret. "
                    + "Kontrollera gärna bokningsnumret i din bokningsbekräftelse "
                    + "eller på Mina sidor och skriv det igen. "
                    + "Bokningsnumret står oftast högst upp i bekräftelsen.";

    private final CustomerChatConversationRepository conversationRepository;
    private final CustomerChatMessageRepository messageRepository;
    private final CustomerChatTicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final TravelRepository travelRepository;
    private final EmailTicketRepository emailTicketRepository;
    private final CustomerSupportKnowledgeBase knowledgeBase;
    private final CustomerChatRealtimeService realtimeService;
    private final CustomerSatisfactionService satisfactionService;
    private final TaskScheduler taskScheduler;
    private final TransactionTemplate transactionTemplate;

    public CustomerChatService(
            CustomerChatConversationRepository conversationRepository,
            CustomerChatMessageRepository messageRepository,
            CustomerChatTicketRepository ticketRepository,
            BookingRepository bookingRepository,
            TravelRepository travelRepository,
            EmailTicketRepository emailTicketRepository,
            CustomerSupportKnowledgeBase knowledgeBase,
            CustomerChatRealtimeService realtimeService,
            CustomerSatisfactionService satisfactionService,
            TaskScheduler taskScheduler,
            TransactionTemplate transactionTemplate
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
        this.travelRepository = travelRepository;
        this.emailTicketRepository = emailTicketRepository;
        this.knowledgeBase = knowledgeBase;
        this.realtimeService = realtimeService;
        this.satisfactionService = satisfactionService;
        this.taskScheduler = taskScheduler;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public CustomerChatConversation startConversation(Principal principal) {
        String email = principal == null ? null : principal.getName();
        String name = email == null ? "Gäst" : email;

        CustomerChatConversation conversation =
                new CustomerChatConversation(name, email);

        CustomerChatConversation savedConversation =
                conversationRepository.save(conversation);

        return savedConversation;
    }

    @Transactional
    public CustomerChatConversation addCustomerMessage(
            String publicId,
            String message,
            Principal principal
    ) {
        CustomerChatConversation conversation =
                getConversationForCustomer(publicId, principal);
        String cleanMessage = message == null ? "" : message.trim();

        if (cleanMessage.isBlank()) {
            throw new IllegalArgumentException("Skriv ett meddelande först.");
        }

        boolean wasClosed =
                conversation.getStatus() == CustomerChatStatus.CLOSED;

        if (wasClosed) {
            conversation.reopen();
        }

        CustomerChatMessage customerMessage = new CustomerChatMessage(
                CustomerChatSender.CUSTOMER,
                customerName(principal),
                cleanMessage
        );
        conversation.addMessage(customerMessage);
        conversation.markCustomerStarted();

        conversation.updateSubject(subjectFrom(cleanMessage));

        BookingChatMatch bookingChatMatch =
                handleBookingContext(
                        conversation,
                        customerMessage,
                        cleanMessage,
                        principal
                );

        if (wasClosed) {
            conversation.addMessage(new CustomerChatMessage(
                    CustomerChatSender.SYSTEM,
                    "Kundtjänst",
                    "Chatten återaktiverades eftersom kunden skrev igen."
            ));
        }

        CustomerChatConversation savedConversation =
                conversationRepository.save(conversation);
        realtimeService.notifyConversation(savedConversation.getPublicId());
        realtimeService.notifyDashboard();

        if (bookingChatMatch.answered()) {
            scheduleBookingAnswer(
                    savedConversation.getId(),
                    savedConversation.getPublicId(),
                    bookingChatMatch.message(),
                    customerServiceAuthorName(savedConversation)
            );
        } else if (!wasClosed && !savedConversation.hasAgentJoined()) {
            scheduleAiAnswer(
                    savedConversation.getId(),
                    savedConversation.getPublicId(),
                    cleanMessage,
                    savedConversation.getCustomerEmail()
            );
        }

        return savedConversation;
    }

    private BookingChatMatch handleBookingContext(
            CustomerChatConversation conversation,
            CustomerChatMessage customerMessage,
            String message,
            Principal principal
    ) {
        Optional<String> bookingNumber = extractBookingNumber(message);

        if (bookingNumber.isPresent()) {
            Optional<Booking> booking = findBookingByCustomerInput(
                    bookingNumber.get()
            );

            if (booking.isPresent()) {
                Booking matchedBooking = booking.get();
                customerMessage.attachDetectedBooking(
                        matchedBooking.getId(),
                        matchedBooking.getBookingNumber()
                );
                linkConversationToBooking(
                        conversation,
                        matchedBooking,
                        matchedBooking.getBookingNumber()
                );
                return new BookingChatMatch(
                        true,
                        confirmationQuestion(matchedBooking)
                );
            }

            return new BookingChatMatch(
                    true,
                    BOOKING_NOT_FOUND_MESSAGE
            );
        }

        if (mentionsBookingNumber(message)) {
            return new BookingChatMatch(
                    true,
                    BOOKING_NOT_FOUND_MESSAGE
            );
        }

        findLoggedInCustomerBookingFromMessage(message, principal)
                .ifPresent(booking -> linkConversationToBooking(
                        conversation,
                        booking,
                        booking.getCustomer().getEmail()
                ));

        return new BookingChatMatch(false, "");
    }

    private Optional<String> extractBookingNumber(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = BOOKING_NUMBER_PATTERN.matcher(message);

        if (!matcher.find()) {
            return Optional.empty();
        }

        return Optional.of(matcher.group().toUpperCase(Locale.ROOT));
    }

    private boolean mentionsBookingNumber(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        return BOOKING_NUMBER_HINT_PATTERN.matcher(message).find();
    }

    private Optional<Booking> findBookingByCustomerInput(
            String bookingNumber
    ) {
        String cleanBookingNumber = bookingNumber == null
                ? ""
                : bookingNumber.trim().toUpperCase(Locale.ROOT);

        if (cleanBookingNumber.isBlank()) {
            return Optional.empty();
        }

        Optional<Booking> booking =
                bookingRepository.findByBookingNumberIgnoreCase(
                        cleanBookingNumber
                );

        if (booking.isPresent() || cleanBookingNumber.startsWith("E")) {
            return booking;
        }

        return bookingRepository.findByBookingNumberIgnoreCase(
                "E" + cleanBookingNumber
        );
    }

    private Optional<Booking> findLoggedInCustomerBookingFromMessage(
            String message,
            Principal principal
    ) {
        String customerEmail = principal == null ? null : principal.getName();

        if (customerEmail == null || customerEmail.isBlank()) {
            return Optional.empty();
        }

        String normalizedMessage = normalize(message);
        String messageDigits = digitsOnly(message);

        return bookingsForCustomerEmail(customerEmail)
                .stream()
                .filter(booking -> {
                    String phone = booking.getCustomer().getPhone();
                    String firstName = booking.getCustomer().getFirstName();
                    String lastName = booking.getCustomer().getLastName();
                    String fullName = (firstName + " " + lastName)
                            .trim();

                    return (!digitsOnly(phone).isBlank()
                            && messageDigits.contains(digitsOnly(phone)))
                            || (!fullName.isBlank()
                            && normalizedMessage.contains(normalize(fullName)))
                            || (!firstName.isBlank()
                            && !lastName.isBlank()
                            && normalizedMessage.contains(normalize(firstName))
                            && normalizedMessage.contains(normalize(lastName)));
                })
                .findFirst();
    }

    private List<Booking> bookingsForCustomerEmail(String customerEmail) {
        LinkedHashMap<Long, Booking> bookings = new LinkedHashMap<>();

        bookingRepository
                .findAllByUserEmailIgnoreCaseOrderByBookedAtDesc(customerEmail)
                .forEach(booking -> bookings.put(booking.getId(), booking));
        bookingRepository
                .findAllByCustomerUserEmailIgnoreCaseOrderByBookedAtDesc(
                        customerEmail
                )
                .forEach(booking -> bookings.putIfAbsent(
                        booking.getId(),
                        booking
                ));

        return bookings
                .values()
                .stream()
                .toList();
    }

    private void linkConversationToBooking(
            CustomerChatConversation conversation,
            Booking booking,
            String registerQuery
    ) {
        conversation.linkBooking(
                booking.getId(),
                booking.getBookingNumber(),
                bookingLabel(booking),
                registerQuery
        );
    }

    private String confirmationQuestion(Booking booking) {
        return "Gäller det "
                + booking.getDeparture().getTravel().getDestination()
                + " med avgång "
                + booking.getDeparture()
                        .getDepartureDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE)
                + "?";
    }

    private String bookingLabel(Booking booking) {
        return booking.getBookingNumber()
                + " · "
                + booking.getDeparture().getTravel().getDestination()
                + " · "
                + booking.getDeparture()
                        .getDepartureDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String digitsOnly(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("\\D+", "");
    }

    private String customerServiceAuthorName(
            CustomerChatConversation conversation
    ) {
        if (conversation.hasAgentJoined()) {
            return firstName(conversation.getAssignedAgentName())
                    + " (Kundtjänst)";
        }

        return "Kundtjänst";
    }

    private String firstName(String name) {
        if (name == null || name.isBlank()) {
            return "Kundtjänst";
        }

        return name.trim().split("\\s+")[0];
    }

    private void scheduleBookingAnswer(
            Long conversationId,
            String publicId,
            String answer,
            String typingName
    ) {
        realtimeService.typing(
                publicId,
                "AGENT",
                typingName,
                true,
                ""
        );

        taskScheduler.schedule(
                () -> transactionTemplate.execute(status -> {
                    CustomerChatConversation conversation =
                            conversationRepository
                                    .findById(conversationId)
                                    .orElse(null);

                    if (conversation == null
                            || conversation.getStatus() == CustomerChatStatus.CLOSED) {
                        realtimeService.typing(
                                publicId,
                                "AGENT",
                                typingName,
                                false,
                                ""
                        );
                        return null;
                    }

                    String authorName = customerServiceAuthorName(conversation);
                    conversation.addMessage(new CustomerChatMessage(
                            conversation.hasAgentJoined()
                                    ? CustomerChatSender.AGENT
                                    : CustomerChatSender.SYSTEM,
                            authorName,
                            answer
                    ));

                    CustomerChatConversation savedConversation =
                            conversationRepository.save(conversation);
                    realtimeService.typing(
                            publicId,
                            "AGENT",
                            authorName,
                            false,
                            ""
                    );
                    realtimeService.notifyConversation(
                            savedConversation.getPublicId()
                    );
                    realtimeService.notifyDashboard();

                    return null;
                }),
                Instant.now().plusSeconds(4)
        );
    }

    private void scheduleAiAnswer(
            Long conversationId,
            String publicId,
            String message,
            String customerEmail
    ) {
        realtimeService.typing(
                publicId,
                "AI",
                "EriGo Assistent",
                true,
                ""
        );

        taskScheduler.schedule(
                () -> transactionTemplate.execute(status -> {
                    CustomerChatConversation conversation =
                            conversationRepository
                                    .findById(conversationId)
                                    .orElse(null);

                    if (conversation == null) {
                        realtimeService.typing(
                                publicId,
                                "AI",
                                "EriGo Assistent",
                                false,
                                ""
                        );
                        return null;
                    }

                    if (conversation.hasAgentJoined()
                            || conversation.getStatus() == CustomerChatStatus.CLOSED) {
                        realtimeService.typing(
                                publicId,
                                "AI",
                                "EriGo Assistent",
                                false,
                                ""
                        );
                        return null;
                    }

                    AiAnswer answer = answerCustomer(message, customerEmail);
                    conversation.addMessage(new CustomerChatMessage(
                            answer.escalate()
                                    ? CustomerChatSender.SYSTEM
                                    : CustomerChatSender.AI,
                            answer.escalate()
                                    ? "Kundtjänst"
                                    : "EriGo Assist",
                            answer.message()
                    ));

                    if (answer.escalate()) {
                        conversation.waitForAgent();
                    }

                    CustomerChatConversation savedConversation =
                            conversationRepository.save(conversation);
                    realtimeService.typing(
                            publicId,
                            "AI",
                            "EriGo Assistent",
                            false,
                            ""
                    );
                    realtimeService.notifyConversation(
                            savedConversation.getPublicId()
                    );
                    realtimeService.notifyDashboard();

                    return null;
                }),
                Instant.now().plusSeconds(3)
        );
    }

    @Transactional
    public CustomerChatConversation addAgentReply(
            Long conversationId,
            String message,
            String agentEmail
    ) {
        CustomerChatConversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Chatten kunde inte hittas."
                        ));

        if (!conversation.hasAgentJoined()) {
            throw new IllegalStateException(
                    "Klicka på Chatta med kund innan du svarar."
            );
        }

        conversation.addMessage(new CustomerChatMessage(
                CustomerChatSender.AGENT,
                agentName(agentEmail),
                message.trim()
        ));

        CustomerChatConversation savedConversation =
                conversationRepository.save(conversation);
        realtimeService.notifyConversation(savedConversation.getPublicId());
        realtimeService.notifyDashboard();

        return savedConversation;
    }

    @Transactional
    public CustomerChatConversation requestCustomerIdentification(
            Long conversationId,
            String agentEmail
    ) {
        CustomerChatConversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Chatten kunde inte hittas."
                        ));

        if (!conversation.hasAgentJoined()) {
            throw new IllegalStateException(
                    "Klicka på Chatta med kund innan du identifierar kunden."
            );
        }

        addIdentificationRequest(
                conversation,
                agentName(agentEmail),
                "Fyll i uppgifterna så kan vi identifiera din bokning."
        );

        CustomerChatConversation savedConversation =
                conversationRepository.save(conversation);
        realtimeService.notifyConversation(savedConversation.getPublicId());
        realtimeService.notifyDashboard();

        return savedConversation;
    }

    private void addIdentificationRequest(
            CustomerChatConversation conversation,
            String authorName,
            String message
    ) {
        CustomerChatMessage requestMessage = new CustomerChatMessage(
                CustomerChatSender.AGENT,
                authorName,
                message
        );
        requestMessage.markAsIdentificationRequest();
        conversation.addMessage(requestMessage);
    }

    @Transactional
    public CustomerChatConversation identifyCustomerBooking(
            String publicId,
            CustomerChatIdentificationRequest request,
            Principal principal
    ) {
        CustomerChatConversation conversation =
                getConversationForCustomer(publicId, principal);
        Optional<CustomerChatMessage> requestMessage =
                findIdentificationRequest(conversation, request);

        if (requestMessage
                .map(CustomerChatMessage::isIdentificationSubmitted)
                .orElse(false)) {
            return conversation;
        }

        String bookingNumber = clean(request.getBookingNumber());
        String customerNumber = clean(request.getCustomerNumber());
        String firstName = clean(request.getFirstName());
        String lastName = clean(request.getLastName());

        if (bookingNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Skriv bokningsnummer för att identifiera bokningen."
            );
        }

        requestMessage.ifPresent(CustomerChatMessage::markIdentificationSubmitted);

        CustomerChatMessage customerMessage = new CustomerChatMessage(
                CustomerChatSender.CUSTOMER,
                customerName(principal),
                identificationSummary(
                        customerNumber,
                        bookingNumber,
                        firstName,
                        lastName
                )
        );

        Optional<Booking> booking = findBookingByCustomerInput(bookingNumber);

        if (booking.isPresent()) {
            Booking matchedBooking = booking.get();
            customerMessage.attachDetectedBooking(
                    matchedBooking.getId(),
                    matchedBooking.getBookingNumber()
            );
            linkConversationToBooking(
                    conversation,
                    matchedBooking,
                    matchedBooking.getBookingNumber()
            );
        }

        conversation.addMessage(customerMessage);

        if (booking.isPresent()) {
            conversation.addMessage(new CustomerChatMessage(
                    CustomerChatSender.SYSTEM,
                    customerServiceAuthorName(conversation),
                    "Tack, vi hittade din bokning."
            ));
        } else {
            addIdentificationRequest(
                    conversation,
                    customerServiceAuthorName(conversation),
                    "Du har skrivit in fel uppgifter, vänligen prova igen. "
                            + "Du hittar dina uppgifter på bokningsbekräftelsemejlet "
                            + "eller på Mina sidor."
            );
        }

        CustomerChatConversation savedConversation =
                conversationRepository.save(conversation);
        realtimeService.notifyConversation(savedConversation.getPublicId());
        realtimeService.notifyDashboard();

        return savedConversation;
    }

    private Optional<CustomerChatMessage> findIdentificationRequest(
            CustomerChatConversation conversation,
            CustomerChatIdentificationRequest request
    ) {
        Long requestMessageId = request.getRequestMessageId();

        if (requestMessageId == null) {
            return Optional.empty();
        }

        return conversation
                .getMessages()
                .stream()
                .filter(CustomerChatMessage::isIdentificationRequest)
                .filter(message -> requestMessageId.equals(message.getId()))
                .findFirst();
    }

    private String identificationSummary(
            String customerNumber,
            String bookingNumber,
            String firstName,
            String lastName
    ) {
        String name = (firstName + " " + lastName).trim();

        return "Identifieringsuppgifter skickade: "
                + "kundnummer "
                + valueOrDash(customerNumber)
                + ", bokningsnummer "
                + valueOrDash(bookingNumber)
                + ", namn "
                + valueOrDash(name)
                + ".";
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    @Transactional
    public CustomerChatConversation editAgentReply(
            Long conversationId,
            Long messageId,
            String message,
            String agentEmail
    ) {
        CustomerChatConversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Chatten kunde inte hittas."
                        ));
        CustomerChatMessage chatMessage =
                messageRepository
                        .findById(messageId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Meddelandet kunde inte hittas."
                        ));
        String cleanMessage = message == null ? "" : message.trim();

        if (conversation.getStatus() == CustomerChatStatus.CLOSED) {
            throw new IllegalStateException(
                    "Det går inte att redigera meddelanden i en avslutad chatt."
            );
        }

        if (!chatMessage.getConversation().getId().equals(conversationId)
                || chatMessage.getSender() != CustomerChatSender.AGENT) {
            throw new IllegalStateException(
                    "Du kan bara redigera agentmeddelanden i den här chatten."
            );
        }

        if (!chatMessage.getAuthorName().equals(agentName(agentEmail))) {
            throw new IllegalStateException(
                    "Du kan bara redigera meddelanden som du själv har skickat."
            );
        }

        if (cleanMessage.isBlank()) {
            throw new IllegalStateException(
                    "Meddelandet får inte vara tomt."
            );
        }

        chatMessage.updateMessage(cleanMessage);
        messageRepository.save(chatMessage);
        CustomerChatConversation savedConversation =
                conversationRepository.save(conversation);
        realtimeService.notifyConversation(savedConversation.getPublicId());
        realtimeService.notifyDashboard();

        return savedConversation;
    }

    @Transactional
    public CustomerChatConversation addAgentNote(
            Long conversationId,
            String note,
            String agentEmail
    ) {
        CustomerChatConversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Chatten kunde inte hittas."
                        ));
        String cleanNote = note == null ? "" : note.trim();

        if (conversation.getStatus() == CustomerChatStatus.CLOSED) {
            throw new IllegalStateException(
                    "Det går inte att lägga anteckningar på en avslutad chatt."
            );
        }

        if (cleanNote.isBlank()) {
            throw new IllegalArgumentException("Skriv en anteckning först.");
        }

        conversation.addMessage(new CustomerChatMessage(
                CustomerChatSender.NOTE,
                agentName(agentEmail),
                cleanNote
        ));

        CustomerChatConversation savedConversation =
                conversationRepository.save(conversation);
        realtimeService.notifyConversation(savedConversation.getPublicId());
        realtimeService.notifyDashboard();

        return savedConversation;
    }

    @Transactional
    public CustomerChatConversation joinConversation(
            Long conversationId,
            String agentEmail
    ) {
        CustomerChatConversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Chatten kunde inte hittas."
                        ));

        if (!conversation.hasAgentJoined()) {
            String agentName = agentName(agentEmail);
            conversation.assignAgent(agentName);
            conversation.addMessage(new CustomerChatMessage(
                    CustomerChatSender.SYSTEM,
                    "Kundtjänst",
                    agentName + " har anslutit sig till chatten."
            ));
        }

        CustomerChatConversation savedConversation =
                conversationRepository.save(conversation);
        realtimeService.notifyConversation(savedConversation.getPublicId());
        realtimeService.notifyDashboard();

        return savedConversation;
    }

    @Transactional
    public void closeConversation(
            Long conversationId,
            String agentEmail
    ) {
        CustomerChatConversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Chatten kunde inte hittas."
                        ));
        String agentName = agentName(agentEmail);
        conversation.addMessage(new CustomerChatMessage(
                CustomerChatSender.SYSTEM,
                "Kundtjänst",
                agentName + " avslutade chatten."
        ));
        conversation.close();
        CustomerChatConversation savedConversation =
                conversationRepository.save(conversation);
        satisfactionService.sendForChat(savedConversation);
        realtimeService.notifyConversation(conversation.getPublicId());
        realtimeService.notifyDashboard();
    }

    @Transactional
    public CustomerChatTicket escalateToTicket(
            Long conversationId,
            String agentEmail
    ) {
        CustomerChatConversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Chatten kunde inte hittas."
                        ));

        return ticketRepository
                .findByConversationId(conversationId)
                .orElseGet(() -> {
                    CustomerChatTicket ticket =
                            ticketRepository.save(new CustomerChatTicket(
                        conversation,
                        ticketTitle(conversation),
                        summarizeConversation(conversation),
                        agentName(agentEmail)
                    ));

                    conversation.addMessage(new CustomerChatMessage(
                            CustomerChatSender.SYSTEM,
                            "Kundtjänst",
                            agentName(agentEmail)
                                    + " eskalerade chatten till rapporter för vidare hantering."
                    ));
                    conversation.escalate();
                    conversation.addMessage(new CustomerChatMessage(
                            CustomerChatSender.SYSTEM,
                            "Kundtjänst",
                            "Chatten avslutades automatiskt eftersom ärendet eskalerades."
                    ));
                    conversation.close();
                    conversationRepository.save(conversation);
                    realtimeService.notifyConversation(conversation.getPublicId());
                    realtimeService.notifyDashboard();

                    return ticket;
                });
    }

    public List<CustomerChatTicket> getTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public CustomerChatConversation closeCustomerConversation(
            String publicId,
            Principal principal
    ) {
        CustomerChatConversation conversation =
                getConversationForCustomer(publicId, principal);
        String customerName = customerName(principal);
        conversation.addMessage(new CustomerChatMessage(
                CustomerChatSender.SYSTEM,
                "Kundtjänst",
                customerName + " avslutade chatten."
        ));
        conversation.close();

        CustomerChatConversation savedConversation =
                conversationRepository.save(conversation);
        realtimeService.notifyConversation(savedConversation.getPublicId());
        realtimeService.notifyDashboard();

        return savedConversation;
    }

    public CustomerChatConversation getConversation(String publicId) {
        return conversationRepository
                .findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Chatten kunde inte hittas."
                ));
    }

    public CustomerChatConversation getConversationForCustomer(
            String publicId,
            Principal principal
    ) {
        CustomerChatConversation conversation = getConversation(publicId);
        String loggedInEmail = principal == null ? null : principal.getName();

        if (loggedInEmail == null) {
            return conversation;
        }

        if (loggedInEmail.equalsIgnoreCase(conversation.getCustomerEmail())) {
            return conversation;
        }

        throw new IllegalArgumentException("Du kan bara öppna din egen chatt.");
    }

    public CustomerChatConversation getAdminConversation(Long id) {
        return conversationRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Chatten kunde inte hittas."
                ));
    }

    @Transactional
    public void archiveConversation(Long conversationId) {
        CustomerChatConversation conversation =
                getAdminConversation(conversationId);

        if (conversation.getStatus() != CustomerChatStatus.CLOSED) {
            throw new IllegalStateException(
                    "Det går bara att arkivera avslutade chattar."
            );
        }

        conversation.archiveNow(ARCHIVE_AFTER_MINUTES);
        conversationRepository.save(conversation);
        realtimeService.notifyDashboard();
    }

    public List<CustomerChatConversation> getConversations(String status) {
        LocalDateTime archiveBefore = LocalDateTime
                .now()
                .minusMinutes(ARCHIVE_AFTER_MINUTES);

        if (status == null || status.isBlank() || "ALL".equals(status)) {
            return conversationRepository
                    .findAllByCustomerStartedTrueOrderByUpdatedAtDesc()
                    .stream()
                    .filter(conversation -> !isArchived(
                            conversation,
                            archiveBefore
                    ))
                    .toList();
        }

        if ("WAITING_FOR_AGENT".equals(status)) {
            return conversationRepository
                    .findAllByCustomerStartedTrueAndStatusOrderByUpdatedAtDesc(
                            CustomerChatStatus.WAITING_FOR_AGENT
                    );
        }

        if ("ARCHIVED".equals(status)) {
            return conversationRepository
                    .findAllByCustomerStartedTrueAndStatusOrderByUpdatedAtDesc(
                            CustomerChatStatus.CLOSED
                    )
                    .stream()
                    .filter(conversation -> isArchived(
                            conversation,
                            archiveBefore
                    ))
                    .toList();
        }

        if ("CLOSED".equals(status)) {
            return conversationRepository
                    .findAllByCustomerStartedTrueAndStatusOrderByUpdatedAtDesc(
                            CustomerChatStatus.CLOSED
                    )
                    .stream()
                    .filter(conversation -> !isArchived(
                            conversation,
                            archiveBefore
                    ))
                    .toList();
        }

        return conversationRepository
                .findAllByCustomerStartedTrueAndStatusOrderByUpdatedAtDesc(
                CustomerChatStatus.valueOf(status)
        );
    }

    public List<CustomerChatConversation> getConversationsForAgent(
            String agentEmail
    ) {
        String agentName = agentName(agentEmail);
        LocalDateTime archiveBefore = LocalDateTime
                .now()
                .minusMinutes(ARCHIVE_AFTER_MINUTES);

        return conversationRepository
                .findAllByCustomerStartedTrueAndAssignedAgentNameIgnoreCaseOrderByUpdatedAtDesc(
                        agentName
                )
                .stream()
                .filter(conversation -> !isArchived(
                        conversation,
                        archiveBefore
                ))
                .toList();
    }

    private boolean isArchived(
            CustomerChatConversation conversation,
            LocalDateTime archiveBefore
    ) {
        return conversation.getStatus() == CustomerChatStatus.CLOSED
                && conversation.getClosedAt() != null
                && conversation.getClosedAt().isBefore(archiveBefore);
    }

    public List<CustomerChatMessage> getMessages(String publicId) {
        return messageRepository
                .findAllByConversationPublicIdOrderByCreatedAtAsc(publicId);
    }

    public AgentQueueOverview getAgentQueueOverview() {
        long openChats =
                conversationRepository.countByCustomerStartedTrueAndStatus(
                        CustomerChatStatus.OPEN
                );
        long waitingForAgentChats =
                conversationRepository.countByCustomerStartedTrueAndStatus(
                        CustomerChatStatus.WAITING_FOR_AGENT
                );
        long escalatedChats =
                conversationRepository.countByCustomerStartedTrueAndStatus(
                        CustomerChatStatus.ESCALATED
                );
        int phoneQueueMinutes =
                (int) Math.min(45, 4 + waitingForAgentChats * 3 + openChats);
        int incomingEmails =
                (int) Math.min(99, emailTicketRepository.countByStatusNot(
                        com.ericthilen.travelbookingplatform.model.EmailTicketStatus.CLOSED
                ));

        return new AgentQueueOverview(
                openChats,
                waitingForAgentChats,
                escalatedChats,
                phoneQueueMinutes,
                incomingEmails
        );
    }

    private AiAnswer answerCustomer(String message, String customerEmail) {
        String normalized = normalize(message);

        if (shouldEscalate(normalized)) {
            return new AiAnswer(
                    knowledgeBase.escalationText(),
                    true
            );
        }

        if (containsAny(normalized, "betal", "faktura", "handpenning", "slutbetal")) {
            return new AiAnswer(withBookingContext(
                    knowledgeBase.paymentHelp(),
                    customerEmail
            ), false);
        }

        if (containsAny(normalized, "avboka", "avbokning", "sjuk", "återbetal", "aterbetal")) {
            return new AiAnswer(withBookingContext(
                    knowledgeBase.cancellationHelp(),
                    customerEmail
            ), false);
        }

        if (containsAny(normalized, "bokning", "bokningsnummer", "kundnummer", "min resa", "din resa")) {
            return new AiAnswer(withBookingContext(
                    knowledgeBase.bookingHelp(),
                    customerEmail
            ), false);
        }

        if (containsAny(normalized, "logga", "konto", "lösenord", "losenord", "profil")) {
            return new AiAnswer(
                    knowledgeBase.accountHelp(),
                    false
            );
        }

        if (containsAny(normalized, "villkor", "cookie", "integritet", "resegaranti", "paketresa")) {
            return new AiAnswer(
                    knowledgeBase.legalHelp(),
                    false
            );
        }

        if (containsAny(normalized, "bagage", "handbagage", "flygtid", "flygnummer", "flyginformation")) {
            return new AiAnswer(
                    knowledgeBase.baggageHelp(),
                    false
            );
        }

        if (containsAny(normalized, "resenär", "resenar", "namn", "personnummer", "passagerare")) {
            return new AiAnswer(withBookingContext(
                    knowledgeBase.travelerHelp(),
                    customerEmail
            ), false);
        }

        if (containsAny(normalized, "rabatt", "rabattkod", "kod", "ergo500")) {
            return new AiAnswer(withBookingContext(
                    knowledgeBase.discountHelp(),
                    customerEmail
            ), false);
        }

        if (containsAny(normalized, "support", "kontakt", "hjälp", "hjalp", "fråga", "fraga")) {
            return new AiAnswer(
                    knowledgeBase.contactHelp(),
                    false
            );
        }

        if (containsAny(normalized, "resa", "resor", "hotell", "destination", "flyg", "avgång", "avgang")) {
            return new AiAnswer(
                    knowledgeBase.travelHelp() + " " + travelSuggestions(),
                    false
            );
        }

        return new AiAnswer(
                "Jag vill gärna hjälpa dig, men jag är inte helt säker på vad du menar. "
                        + "Skriv gärna om det gäller resa, bokning, betalning, faktura, "
                        + "avbokning eller konto. Vill du prata med en människa kan du skriva "
                        + "\"agent\".",
                false
        );
    }

    private String withBookingContext(String answer, String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            return answer + " Om du loggar in kan jag även se dina bokningar och ge mer personlig hjälp.";
        }

        List<Booking> bookings =
                bookingRepository
                        .findAllByUserEmailIgnoreCaseOrderByBookedAtDesc(
                                customerEmail
                        );

        if (bookings.isEmpty()) {
            return answer + " Jag hittar inga bokningar kopplade till ditt konto just nu.";
        }

        Booking latestBooking = bookings.getFirst();

        return answer + " Jag ser att din senaste bokning är "
                + latestBooking.getBookingNumber()
                + " till "
                + latestBooking.getDeparture().getTravel().getDestination()
                + ", avresa "
                + latestBooking.getDeparture().getDepartureDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE)
                + ". Betalningsstatus: "
                + latestBooking.getPaymentStatus().getDisplayName()
                + ".";
    }

    private String travelSuggestions() {
        String suggestions = travelRepository
                .findAll()
                .stream()
                .limit(3)
                .map(travel -> travel.getDestination()
                        + " på "
                        + travel.getHotelName()
                        + " från "
                        + String.format(
                                Locale.forLanguageTag("sv-SE"),
                                "%,d",
                                travel.getPrice()
                        ).replace(",", " ")
                        + " kr")
                .reduce((first, second) -> first + ", " + second)
                .orElse("");

        if (suggestions.isBlank()) {
            return "";
        }

        return "Exempel på aktuella resor: " + suggestions + ".";
    }

    private boolean shouldEscalate(String message) {
        return containsAny(
                message,
                "agent",
                "människa",
                "manniska",
                "kundtjänst",
                "kundtjanst",
                "ring mig",
                "klagomål",
                "klagomal",
                "ersättning",
                "ersattning",
                "feldebiter",
                "akut"
        );
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String message) {
        return message
                .toLowerCase(Locale.forLanguageTag("sv-SE"))
                .trim();
    }

    private String customerName(Principal principal) {
        if (principal == null) {
            return "Kund";
        }

        return principal.getName();
    }

    private String agentName(String agentEmail) {
        if (agentEmail == null || agentEmail.isBlank()) {
            return "En kundtjänstagent";
        }

        String name = agentEmail.split("@")[0]
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ")
                .trim();

        if (name.isBlank()) {
            return "En kundtjänstagent";
        }

        String[] parts = name.split("\\s+");
        StringBuilder formattedName = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!formattedName.isEmpty()) {
                formattedName.append(" ");
            }

            formattedName
                    .append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }

        return formattedName.toString();
    }

    private String subjectFrom(String message) {
        if (message.length() <= 80) {
            return message;
        }

        return message.substring(0, 77) + "...";
    }

    private String ticketTitle(CustomerChatConversation conversation) {
        if (conversation.getSubject() != null
                && !conversation.getSubject().isBlank()) {
            return conversation.getSubject();
        }

        return "Eskalerad kundchatt";
    }

    private String summarizeConversation(CustomerChatConversation conversation) {
        String customerMessages = conversation
                .getMessages()
                .stream()
                .filter(message -> message.getSender() == CustomerChatSender.CUSTOMER)
                .map(CustomerChatMessage::getMessage)
                .limit(4)
                .reduce((first, second) -> first + " " + second)
                .orElse("Kunden har inte lämnat någon tydlig beskrivning.");

        String agentMessages = conversation
                .getMessages()
                .stream()
                .filter(message -> message.getSender() == CustomerChatSender.AGENT)
                .map(CustomerChatMessage::getMessage)
                .limit(4)
                .reduce((first, second) -> first + " " + second)
                .orElse("Agenten har ännu inte dokumenterat någon lösning.");

        return "AI-sammanfattning: Kunden kontaktade kundtjänst om "
                + shortText(customerMessages, 260)
                + " Agentens hantering hittills: "
                + shortText(agentMessages, 260)
                + " Rekommenderad nästa åtgärd: följ upp ärendet manuellt, "
                + "kontrollera kundens bokning och återkoppla med tydligt besked.";
    }

    private String shortText(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String cleanText = text.trim().replaceAll("\\s+", " ");

        if (cleanText.length() <= maxLength) {
            return cleanText;
        }

        return cleanText.substring(0, maxLength - 3) + "...";
    }

    private record AiAnswer(
            String message,
            boolean escalate
    ) {
    }

    private record BookingChatMatch(
            boolean answered,
            String message
    ) {
    }

    public record AgentQueueOverview(
            long openChats,
            long waitingForAgentChats,
            long escalatedChats,
            int phoneQueueMinutes,
            int incomingEmails
    ) {
    }
}
