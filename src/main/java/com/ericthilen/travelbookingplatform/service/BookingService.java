package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.dto.BookingConfirmationRequest;
import com.ericthilen.travelbookingplatform.dto.BookingLookupRequest;
import com.ericthilen.travelbookingplatform.dto.BookingSession;
import com.ericthilen.travelbookingplatform.dto.CancellationSummary;
import com.ericthilen.travelbookingplatform.dto.TravelerRequest;
import com.ericthilen.travelbookingplatform.dto.BookingSummary;
import com.ericthilen.travelbookingplatform.legal.LegalDocumentVersions;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.BookingEvent;
import com.ericthilen.travelbookingplatform.model.BookingStatus;
import com.ericthilen.travelbookingplatform.model.Customer;
import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.model.EmailStatus;
import com.ericthilen.travelbookingplatform.model.Invoice;
import com.ericthilen.travelbookingplatform.model.ManagementStatus;
import com.ericthilen.travelbookingplatform.model.PaymentPlan;
import com.ericthilen.travelbookingplatform.model.RoomType;
import com.ericthilen.travelbookingplatform.model.Traveler;
import com.ericthilen.travelbookingplatform.model.User;
import com.ericthilen.travelbookingplatform.repository.BookingRepository;
import com.ericthilen.travelbookingplatform.repository.BookingEventRepository;
import com.ericthilen.travelbookingplatform.repository.CustomerRepository;
import com.ericthilen.travelbookingplatform.repository.DepartureRepository;
import com.ericthilen.travelbookingplatform.repository.RoomTypeRepository;
import com.ericthilen.travelbookingplatform.repository.UserRepository;
import com.ericthilen.travelbookingplatform.model.Payment;
import com.ericthilen.travelbookingplatform.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class BookingService {

    private static final String ERGO500_CODE = "ERGO500";
    private static final int ERGO500_AMOUNT = 500;

    private final BookingRepository bookingRepository;
    private final BookingEventRepository bookingEventRepository;
    private final CustomerRepository customerRepository;
    private final DepartureRepository departureRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final BookingEmailService bookingEmailService;
    private final Random random = new Random();

    public BookingService(
            BookingRepository bookingRepository,
            BookingEventRepository bookingEventRepository,
            CustomerRepository customerRepository,
            DepartureRepository departureRepository,
            RoomTypeRepository roomTypeRepository,
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            InvoiceService invoiceService,
            BookingEmailService bookingEmailService
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingEventRepository = bookingEventRepository;
        this.customerRepository = customerRepository;
        this.departureRepository = departureRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceService = invoiceService;
        this.bookingEmailService = bookingEmailService;
    }

    @Transactional
    public Booking createBooking(
            BookingSession bookingSession,
            BookingConfirmationRequest confirmationRequest,
            String authenticatedEmail
    ) {
        Departure departure = departureRepository
                .findById(bookingSession.getDepartureId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Avgången kunde inte hittas."
                        )
                );

        RoomType roomType = roomTypeRepository
                .findById(bookingSession.getRoomTypeId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Rumstypen kunde inte hittas."
                        )
                );

        validateAvailability(
                departure,
                roomType,
                bookingSession
        );

        User user =
                findAuthenticatedUser(authenticatedEmail);

        Customer customer = findOrCreateCustomer(
                bookingSession,
                user
        );

        int originalTotalPrice = calculateTotalPrice(
                departure,
                roomType,
                bookingSession
        );
        int discountAmount = validSessionDiscountAmount(
                bookingSession,
                originalTotalPrice
        );
        int totalPrice = originalTotalPrice - discountAmount;

        PaymentInformation paymentInformation =
                calculatePaymentInformation(
                        totalPrice,
                        departure.getDepartureDate()
                );

        Booking booking = new Booking(
                generateBookingNumber(),
                customer,
                user,
                departure,
                roomType,
                bookingSession.getNumberOfTravelers(),
                bookingSession.getNumberOfRooms(),
                totalPrice,
                LocalDateTime.now(),
                confirmationRequest.getDiscoverySource(),
                LegalDocumentVersions.CURRENT_TERMS_VERSION,
                LocalDateTime.now(),
                paymentInformation.paymentPlan(),
                paymentInformation.depositAmount(),
                paymentInformation.depositDueDate(),
                paymentInformation.remainingAmount(),
                paymentInformation.finalPaymentDueDate()
        );
        if (discountAmount > 0) {
            booking.applyDiscount(
                    bookingSession.getDiscountName(),
                    discountAmount,
                    originalTotalPrice,
                    totalPrice,
                    paymentInformation.depositAmount(),
                    paymentInformation.depositDueDate(),
                    paymentInformation.remainingAmount(),
                    paymentInformation.finalPaymentDueDate()
            );
        }
        booking.updateRoomDistribution(
                roomDistributionLabel(
                        bookingSession.getRoomOccupancies()
                )
        );

        for (TravelerRequest travelerRequest
                : bookingSession.getTravelers()) {

            Traveler traveler = new Traveler(
                    travelerRequest.getPersonalNumber(),
                    travelerRequest.getFirstName(),
                    travelerRequest.getLastName()
            );

            booking.addTraveler(traveler);
        }

        departure.reserveSeats(
                bookingSession.getNumberOfTravelers()
        );

        roomType.reserveRooms(
                bookingSession.getNumberOfRooms()
        );

        departureRepository.save(departure);
        roomTypeRepository.save(roomType);

        Booking savedBooking =
                bookingRepository.saveAndFlush(booking);

        // Process payment if provided in request
        if (confirmationRequest.getPaymentMethod() != null) {
            int paymentAmount = paymentInformation.depositAmount();
            if (paymentInformation.paymentPlan() == PaymentPlan.FULL_PAYMENT) {
                paymentAmount = totalPrice;
            }

            if (paymentAmount > 0) {
                String reference = generatePaymentReference(confirmationRequest.getPaymentMethod());

                savedBooking.registerPayment(paymentAmount);
                bookingRepository.saveAndFlush(savedBooking);

                Payment payment = new Payment(
                        savedBooking,
                        paymentAmount,
                        LocalDate.now(),
                        confirmationRequest.getPaymentMethod(),
                        reference,
                        LocalDateTime.now()
                );
                paymentRepository.save(payment);
            }
        }

        Invoice invoice =
                invoiceService.createInvoice(savedBooking);

        EmailStatus emailStatus =
                bookingEmailService.sendBookingConfirmation(
                        savedBooking,
                        invoice
                );

        savedBooking.markBookingEmailStatus(emailStatus);
        logEvent(
                savedBooking,
                "Bokning skapad",
                "Bokningen skapades och fick bokningsnummer "
                        + savedBooking.getBookingNumber()
                        + ".",
                "System"
        );
        logEvent(
                savedBooking,
                "Bokningsmejl skickat",
                "Status efter utskick: "
                        + emailStatus.getDisplayName()
                        + ".",
                "System"
        );

        return bookingRepository.saveAndFlush(savedBooking);
    }

    public List<Booking> getBookingsForUser(
            String email
    ) {
        connectMatchingGuestBookings(email);

        return bookingRepository
                .findAllByUserEmailIgnoreCaseOrderByBookedAtDesc(
                        email
                );
    }

    public Optional<Booking> getBookingForUser(
            Long bookingId,
            String email
    ) {
        if (bookingId == null
                || email == null
                || email.isBlank()) {

            return Optional.empty();
        }

        connectMatchingGuestBookings(email);

        return bookingRepository
                .findByIdAndUserEmailIgnoreCase(
                        bookingId,
                        email.trim()
                );
    }

    public Optional<Booking> findBooking(
            BookingLookupRequest lookupRequest
    ) {
        String customerNumber = lookupRequest
                .getCustomerNumber()
                .trim();

        String bookingNumber = lookupRequest
                .getBookingNumber()
                .trim();

        String email = lookupRequest
                .getEmail()
                .trim()
                .toLowerCase();

        return bookingRepository
                .findByBookingNumberIgnoreCaseAndCustomerCustomerNumberIgnoreCaseAndCustomerEmailIgnoreCase(
                        bookingNumber,
                        customerNumber,
                        email
                );
    }

    public CancellationSummary calculateCancellation(
            Booking booking
    ) {
        if (booking.getStatus()
                == BookingStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Bokningen är redan avbokad."
            );
        }

        LocalDate today = LocalDate.now();

        long daysUntilDeparture =
                ChronoUnit.DAYS.between(
                        today,
                        booking.getDeparture()
                                .getDepartureDate()
                );

        int normalCancellationFee;

        if (daysUntilDeparture >= 15) {
            normalCancellationFee = Math.round(
                    booking.getTotalPrice() * 0.50f
            );
        } else {
            normalCancellationFee =
                    booking.getTotalPrice();
        }

        boolean depositLocked =
                booking.getDepositDueDate() != null
                        && booking.getDepositDueDate()
                        .isBefore(today)
                        && booking.getPaidAmount()
                        >= booking.getDepositAmount()
                        && booking.getDepositAmount() > 0;

        int nonRefundableDeposit = 0;

        if (depositLocked) {
            nonRefundableDeposit = Math.min(
                    booking.getDepositAmount(),
                    booking.getPaidAmount()
            );
        }

        int amountThatMustBeRetained = Math.max(
                normalCancellationFee,
                nonRefundableDeposit
        );

        int actualCancellationFee = Math.min(
                amountThatMustBeRetained,
                booking.getPaidAmount()
        );

        int refundAmount = Math.max(
                0,
                booking.getPaidAmount()
                        - actualCancellationFee
        );

        return new CancellationSummary(
                daysUntilDeparture,
                booking.getPaidAmount(),
                actualCancellationFee,
                refundAmount,
                0,
                nonRefundableDeposit,
                depositLocked
        );
    }

    @Transactional
    public Booking cancelBooking(
            Long bookingId,
            String email
    ) {
        return cancelBooking(
                bookingId,
                email,
                "Ej angiven"
        );
    }

    @Transactional
    public Booking cancelBooking(
            Long bookingId,
            String email,
            String cancellationReason
    ) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Användaren kunde inte identifieras."
            );
        }

        Booking booking = bookingRepository
                .findByIdAndUserEmailIgnoreCase(
                        bookingId,
                        email.trim()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bokningen kunde inte hittas."
                        )
                );

        return cancelExistingBooking(
                booking,
                "Kund",
                cancellationReason
        );
    }

    @Transactional
    public Booking cancelBookingForAdmin(
            Long bookingId,
            String adminEmail,
            String cancellationReason
    ) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bokningen kunde inte hittas."
                        )
                );

        return cancelExistingBooking(
                booking,
                adminEmail == null || adminEmail.isBlank()
                        ? "Admin"
                        : adminEmail,
                cancellationReason
        );
    }

    private Booking cancelExistingBooking(
            Booking booking,
            String actor,
            String cancellationReason
    ) {
        CancellationSummary cancellationSummary =
                calculateCancellation(booking);

        booking.getDeparture().releaseSeats(
                booking.getNumberOfTravelers()
        );

        booking.getRoomType().releaseRooms(
                booking.getNumberOfRooms()
        );

        booking.cancel(
                cancellationSummary.getCancellationFee(),
                cancellationSummary.getRefundAmount(),
                cleanCancellationReason(cancellationReason)
        );

        departureRepository.save(
                booking.getDeparture()
        );

        roomTypeRepository.save(
                booking.getRoomType()
        );

        Booking cancelledBooking =
                bookingRepository.saveAndFlush(booking);

        EmailStatus emailStatus =
                bookingEmailService
                        .sendCancellationConfirmation(
                                cancelledBooking
                        );

        cancelledBooking.markCancellationEmailStatus(
                emailStatus
        );
        logEvent(
                cancelledBooking,
                "Bokning avbokad",
                "Bokningen avbokades. Avbokningsavgift: "
                        + cancellationSummary.getCancellationFee()
                        + " kr. Orsak: "
                        + cleanCancellationReason(cancellationReason)
                        + ".",
                actor
        );

        return bookingRepository
                .saveAndFlush(cancelledBooking);
    }

    private String cleanCancellationReason(String cancellationReason) {
        if (cancellationReason == null
                || cancellationReason.isBlank()) {
            return "Ej angiven";
        }

        return cancellationReason.trim();
    }

    private Customer findOrCreateCustomer(
            BookingSession bookingSession,
            User user
    ) {
        // Först, försök hitta kunden via personnumret
        Optional<Customer> existingByPersonalNumber =
                customerRepository.findByPersonalNumber(
                        bookingSession
                                .getResponsiblePersonalNumber()
                );

        if (existingByPersonalNumber.isPresent()) {
            Customer customer =
                    existingByPersonalNumber.get();

            customer.updateContactInformation(
                    bookingSession.getResponsibleFirstName(),
                    bookingSession.getResponsibleLastName(),
                    bookingSession.getResponsiblePhone(),
                    bookingSession.getResponsibleEmail()
            );

            // Om kunden inte har en användare kopplad, eller om användaren är en annan,
            // så kopplar vi den nya (om den inte är null).
            // Men Customer.user är unikt, så om användaren redan har en kund profil kopplad
            // så kommer detta krocka senare om vi inte är försiktiga.
            customer.connectUser(user);

            return customerRepository.save(customer);
        }

        // Om kunden inte hittades på personnummer, men vi har en inloggad användare,
        // kolla om användaren redan har en kundprofil.
        if (user != null) {
            Optional<Customer> existingByUser =
                    customerRepository.findByUser(user);

            if (existingByUser.isPresent()) {
                Customer customer = existingByUser.get();

                // Här uppdaterar vi profilen med de nya uppgifterna
                // OBS: Vi behåller det gamla personnumret på profilen om det skiljer sig,
                // eller så uppdaterar vi det? Eftersom det är en profil för användaren.
                // Om de har bytt personnummer (ovanligt) eller bara skrivit fel.
                // Vi väljer att uppdatera personnumret också om det är deras profil.
                customer.updateContactInformation(
                        bookingSession.getResponsibleFirstName(),
                        bookingSession.getResponsibleLastName(),
                        bookingSession.getResponsiblePhone(),
                        bookingSession.getResponsibleEmail()
                );
                
                // Vi kan behöva en metod för att uppdatera personnummer om det ändrats.
                // Men för enkelhetens skull i denna domän antar vi att personnummer på profilen
                // bör vara det som anges i bokningen om det är samma användare.
                // Dock är personalNumber unikt i databasen, så om det nya personnumret
                // redan finns på en annan profil kommer det smälla.
                
                return customerRepository.save(customer);
            }
        }

        Customer customer = new Customer(
                generateCustomerNumber(),
                bookingSession.getResponsiblePersonalNumber(),
                bookingSession.getResponsibleFirstName(),
                bookingSession.getResponsibleLastName(),
                bookingSession.getResponsiblePhone(),
                bookingSession.getResponsibleEmail(),
                user
        );

        return customerRepository.save(customer);
    }

    private User findAuthenticatedUser(
            String authenticatedEmail
    ) {
        if (authenticatedEmail == null
                || authenticatedEmail.isBlank()
                || authenticatedEmail.equals(
                "anonymousUser"
        )) {
            return null;
        }

        return userRepository
                .findByEmailIgnoreCase(
                        authenticatedEmail
                )
                .orElse(null);
    }

    private void connectMatchingGuestBookings(String email) {
        User user = findAuthenticatedUser(email);

        if (user == null) {
            return;
        }

        Optional<Customer> customer =
                customerRepository.findByUser(user);

        if (customer.isEmpty()) {
            return;
        }

        List<Booking> guestBookings =
                bookingRepository
                        .findAllByUserIsNullAndCustomerPersonalNumberAndCustomerEmailIgnoreCase(
                                customer.get().getPersonalNumber(),
                                customer.get().getEmail()
                        );

        for (Booking booking : guestBookings) {
            booking.connectUser(user);
            booking.getCustomer().connectUser(user);
        }

        bookingRepository.saveAll(guestBookings);
    }

    private void validateAvailability(
            Departure departure,
            RoomType roomType,
            BookingSession bookingSession
    ) {
        if (!roomType.getTravel()
                .getId()
                .equals(
                        departure.getTravel().getId()
                )) {

            throw new IllegalArgumentException(
                    "Rumstypen tillhör inte den valda resan."
            );
        }

        if (departure.getStatus() != ManagementStatus.ACTIVE
                || departure.getTravel().getStatus() != ManagementStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Den valda avgången är inte öppen för bokning."
            );
        }

        if (roomType.getStatus() != ManagementStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Den valda rumstypen är inte öppen för bokning."
            );
        }

        if (departure.getAvailableSeats()
                < bookingSession.getNumberOfTravelers()) {

            throw new IllegalStateException(
                    "Det finns inte längre tillräckligt "
                            + "många platser."
            );
        }

        if (roomType.getAvailableRooms()
                < bookingSession.getNumberOfRooms()) {

            throw new IllegalStateException(
                    "Det finns inte längre tillräckligt "
                            + "många rum."
            );
        }

        for (Integer occupancy
                : bookingSession.getRoomOccupancies()) {

            if (occupancy == null
                    || occupancy < 1
                    || occupancy
                    > roomType.getMaxGuests()) {

                throw new IllegalStateException(
                        "Rumsfördelningen passar inte den "
                                + "valda rumstypen."
                );
            }
        }
    }

    private String roomDistributionLabel(List<Integer> roomOccupancies) {
        if (roomOccupancies == null || roomOccupancies.isEmpty()) {
            return "";
        }

        List<String> rooms = new java.util.ArrayList<>();

        for (int index = 0; index < roomOccupancies.size(); index++) {
            Integer occupancy = roomOccupancies.get(index);

            if (occupancy == null) {
                continue;
            }

            rooms.add("Rum "
                    + (index + 1)
                    + ": "
                    + occupancy
                    + " gäster");
        }

        return String.join(
                ", ",
                rooms
        );
    }

    private void logEvent(
            Booking booking,
            String title,
            String description,
            String createdBy
    ) {
        bookingEventRepository.save(new BookingEvent(
                booking,
                title,
                description,
                createdBy
        ));
    }

    public BookingSummary calculateBookingSummary(BookingSession session) {
        if (session.getDepartureId() == null || session.getRoomTypeId() == null) {
            return null;
        }

        Departure departure = departureRepository.findById(session.getDepartureId()).orElse(null);
        RoomType roomType = roomTypeRepository.findById(session.getRoomTypeId()).orElse(null);

        if (departure == null || roomType == null) {
            return null;
        }

        int travelPrice = departure.getPricePerPerson() * session.getNumberOfTravelers();
        int roomSupplement = roomType.getPriceSupplementPerRoom() * session.getNumberOfRooms();
        int originalTotalPrice = travelPrice + roomSupplement;
        int discountAmount =
                validSessionDiscountAmount(session, originalTotalPrice);
        int totalPrice = originalTotalPrice - discountAmount;

        PaymentInformation paymentInfo = calculatePaymentInformation(totalPrice, departure.getDepartureDate());

        boolean immediatePaymentRequired = paymentInfo.paymentPlan() == PaymentPlan.IMMEDIATE_DEPOSIT ||
                paymentInfo.paymentPlan() == PaymentPlan.FULL_PAYMENT;

        return new BookingSummary(
                departure,
                roomType,
                session.getNumberOfTravelers(),
                session.getNumberOfRooms(),
                travelPrice,
                roomSupplement,
                session.getDiscountName(),
                discountAmount,
                totalPrice,
                paymentInfo.depositAmount(),
                immediatePaymentRequired
        );
    }

    private int calculateTotalPrice(
            Departure departure,
            RoomType roomType,
            BookingSession bookingSession
    ) {
        int travelPrice =
                departure.getPricePerPerson()
                        * bookingSession
                        .getNumberOfTravelers();

        int roomSupplement =
                roomType.getPriceSupplementPerRoom()
                        * bookingSession
                        .getNumberOfRooms();

        return travelPrice + roomSupplement;
    }

    private PaymentInformation calculatePaymentInformation(
            int totalPrice,
            LocalDate departureDate
    ) {
        LocalDate bookingDate =
                LocalDate.now();

        long daysUntilDeparture =
                ChronoUnit.DAYS.between(
                        bookingDate,
                        departureDate
                );

        if (daysUntilDeparture <= 21) {
            return new PaymentInformation(
                    PaymentPlan.FULL_PAYMENT,
                    0,
                    null,
                    totalPrice,
                    bookingDate
            );
        }

        int depositAmount =
                Math.round(totalPrice * 0.15f);

        int remainingAmount =
                totalPrice - depositAmount;

        if (daysUntilDeparture <= 60) {
            return new PaymentInformation(
                    PaymentPlan.IMMEDIATE_DEPOSIT,
                    depositAmount,
                    bookingDate,
                    remainingAmount,
                    bookingDate.plusDays(7)
            );
        }

        return new PaymentInformation(
                PaymentPlan.STANDARD_DEPOSIT,
                depositAmount,
                bookingDate.plusDays(7),
                remainingAmount,
                bookingDate.plusDays(30)
        );
    }

    public void applyDiscountToSession(
            BookingSession bookingSession,
            String discountCode
    ) {
        Discount discount = getDiscount(discountCode);
        int originalTotalPrice = previewTotalPrice(bookingSession);

        bookingSession.applyDiscount(
                discount.code(),
                discount.name(),
                Math.min(discount.amount(), originalTotalPrice)
        );
    }

    @Transactional
    public void applyCustomerDiscountToBooking(
            Long bookingId,
            String email,
            String discountCode
    ) {
        Booking booking = getBookingForUser(bookingId, email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bokningen kunde inte hittas."
                        )
                );

        if (booking.getPaidAmount() > 0) {
            throw new IllegalStateException(
                    "Rabattkod kan bara läggas till innan något har betalats."
            );
        }

        applyDiscountToBooking(
                booking,
                discountCode,
                "Kund"
        );
    }

    @Transactional
    public void applyAdminDiscountToBooking(
            Long bookingId,
            String discountCode
    ) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bokningen kunde inte hittas."
                        )
                );

        applyDiscountToBooking(
                booking,
                discountCode,
                "Admin"
        );
    }

    private void applyDiscountToBooking(
            Booking booking,
            String discountCode,
            String createdBy
    ) {
        Discount discount = getDiscount(discountCode);
        int originalTotalPrice = booking.getOriginalTotalPrice();
        int discountAmount =
                Math.min(discount.amount(), originalTotalPrice);
        int newTotalPrice =
                originalTotalPrice - discountAmount;
        PaymentInformation paymentInformation =
                calculatePaymentInformation(
                        newTotalPrice,
                        booking.getDeparture().getDepartureDate()
                );

        booking.applyDiscount(
                discount.name(),
                discountAmount,
                originalTotalPrice,
                newTotalPrice,
                paymentInformation.depositAmount(),
                paymentInformation.depositDueDate(),
                paymentInformation.remainingAmount(),
                paymentInformation.finalPaymentDueDate()
        );

        Booking savedBooking = bookingRepository.save(booking);
        logEvent(
                savedBooking,
                "Rabattkod tillagd",
                createdBy
                        + " lade till rabattkod "
                        + discount.name()
                        + " som gav "
                        + discountAmount
                        + " kr rabatt.",
                createdBy
        );
    }

    private int previewTotalPrice(BookingSession bookingSession) {
        Departure departure = departureRepository
                .findById(bookingSession.getDepartureId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Avgången kunde inte hittas."
                        )
                );
        RoomType roomType = roomTypeRepository
                .findById(bookingSession.getRoomTypeId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Rumstypen kunde inte hittas."
                        )
                );

        return calculateTotalPrice(
                departure,
                roomType,
                bookingSession
        );
    }

    private int validSessionDiscountAmount(
            BookingSession bookingSession,
            int originalTotalPrice
    ) {
        if (bookingSession.getDiscountAmount() <= 0
                || bookingSession.getDiscountCode() == null
                || bookingSession.getDiscountCode().isBlank()) {
            return 0;
        }

        Discount discount = getDiscount(bookingSession.getDiscountCode());

        return Math.min(
                discount.amount(),
                originalTotalPrice
        );
    }

    private Discount getDiscount(String discountCode) {
        String cleanedCode =
                discountCode == null
                        ? ""
                        : discountCode.trim().toUpperCase();

        if (!ERGO500_CODE.equals(cleanedCode)) {
            throw new IllegalArgumentException(
                    "Rabattkoden kunde inte hittas."
            );
        }

        return new Discount(
                ERGO500_CODE,
                "ERGO500",
                ERGO500_AMOUNT
        );
    }

    private String generateCustomerNumber() {
        String customerNumber;

        do {
            customerNumber =
                    "ERG-" + randomNumberPart();
        } while (customerRepository
                .existsByCustomerNumber(
                        customerNumber
                ));

        return customerNumber;
    }

    private String generateBookingNumber() {
        String bookingNumber;

        do {
            bookingNumber = "E"
                    + String.format(
                    "%07d",
                    random.nextInt(10_000_000)
            );
        } while (bookingRepository
                .existsByBookingNumber(
                        bookingNumber
                ));

        return bookingNumber;
    }

    private String randomNumberPart() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String generatePaymentReference(com.ericthilen.travelbookingplatform.model.PaymentMethod method) {
        String prefix = switch (method) {
            case CARD -> "CARD-";
            case SWISH -> "SWSH-";
            default -> "PAY-";
        };
        return prefix + String.format("%08d", random.nextInt(100_000_000));
    }

    private record PaymentInformation(
            PaymentPlan paymentPlan,
            int depositAmount,
            LocalDate depositDueDate,
            int remainingAmount,
            LocalDate finalPaymentDueDate
    ) {
    }

    private record Discount(
            String code,
            String name,
            int amount
    ) {
    }
}
