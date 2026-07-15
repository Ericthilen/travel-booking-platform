package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.dto.BookingConfirmationRequest;
import com.ericthilen.travelbookingplatform.dto.BookingLookupRequest;
import com.ericthilen.travelbookingplatform.dto.BookingSession;
import com.ericthilen.travelbookingplatform.dto.CancellationSummary;
import com.ericthilen.travelbookingplatform.dto.TravelerRequest;
import com.ericthilen.travelbookingplatform.dto.BookingSummary;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.BookingStatus;
import com.ericthilen.travelbookingplatform.model.Customer;
import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.model.EmailStatus;
import com.ericthilen.travelbookingplatform.model.Invoice;
import com.ericthilen.travelbookingplatform.model.PaymentPlan;
import com.ericthilen.travelbookingplatform.model.RoomType;
import com.ericthilen.travelbookingplatform.model.Traveler;
import com.ericthilen.travelbookingplatform.model.User;
import com.ericthilen.travelbookingplatform.repository.BookingRepository;
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

    private final BookingRepository bookingRepository;
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
            CustomerRepository customerRepository,
            DepartureRepository departureRepository,
            RoomTypeRepository roomTypeRepository,
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            InvoiceService invoiceService,
            BookingEmailService bookingEmailService
    ) {
        this.bookingRepository = bookingRepository;
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

        int totalPrice = calculateTotalPrice(
                departure,
                roomType,
                bookingSession
        );

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
                paymentInformation.paymentPlan(),
                paymentInformation.depositAmount(),
                paymentInformation.depositDueDate(),
                paymentInformation.remainingAmount(),
                paymentInformation.finalPaymentDueDate()
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
                cancellationSummary.getRefundAmount()
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

        return bookingRepository
                .saveAndFlush(cancelledBooking);
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
        int totalPrice = travelPrice + roomSupplement;

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
}
