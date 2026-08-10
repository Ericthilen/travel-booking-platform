package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.Booking;
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

@Service
public class CustomerChatService {

    private static final int ARCHIVE_AFTER_MINUTES = 15;

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

        conversation.addMessage(new CustomerChatMessage(
                CustomerChatSender.CUSTOMER,
                customerName(principal),
                cleanMessage
        ));
        conversation.markCustomerStarted();

        conversation.updateSubject(subjectFrom(cleanMessage));

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

        if (!wasClosed && !savedConversation.hasAgentJoined()) {
            scheduleAiAnswer(
                    savedConversation.getId(),
                    savedConversation.getPublicId(),
                    cleanMessage,
                    savedConversation.getCustomerEmail()
            );
        }

        return savedConversation;
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

    public record AgentQueueOverview(
            long openChats,
            long waitingForAgentChats,
            long escalatedChats,
            int phoneQueueMinutes,
            int incomingEmails
    ) {
    }
}
