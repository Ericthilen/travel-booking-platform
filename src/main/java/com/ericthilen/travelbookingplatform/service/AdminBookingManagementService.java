package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.dto.AdminBookingContactRequest;
import com.ericthilen.travelbookingplatform.dto.AdminNoteRequest;
import com.ericthilen.travelbookingplatform.dto.AdminTravelerNameRequest;
import com.ericthilen.travelbookingplatform.model.AdminNote;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.BookingEvent;
import com.ericthilen.travelbookingplatform.model.BookingStatus;
import com.ericthilen.travelbookingplatform.model.EmailStatus;
import com.ericthilen.travelbookingplatform.model.Invoice;
import com.ericthilen.travelbookingplatform.model.Traveler;
import com.ericthilen.travelbookingplatform.repository.AdminNoteRepository;
import com.ericthilen.travelbookingplatform.repository.BookingEventRepository;
import com.ericthilen.travelbookingplatform.repository.BookingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class AdminBookingManagementService {

    private static final int CHANGE_LOCK_DAYS_BEFORE_DEPARTURE = 7;

    private final BookingRepository bookingRepository;
    private final BookingEventRepository bookingEventRepository;
    private final AdminNoteRepository adminNoteRepository;
    private final InvoiceService invoiceService;
    private final BookingEmailService bookingEmailService;

    public AdminBookingManagementService(
            BookingRepository bookingRepository,
            BookingEventRepository bookingEventRepository,
            AdminNoteRepository adminNoteRepository,
            InvoiceService invoiceService,
            BookingEmailService bookingEmailService
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingEventRepository = bookingEventRepository;
        this.adminNoteRepository = adminNoteRepository;
        this.invoiceService = invoiceService;
        this.bookingEmailService = bookingEmailService;
    }

    public List<BookingEvent> getEvents(Long bookingId) {
        return bookingEventRepository
                .findAllByBookingIdOrderByCreatedAtDesc(bookingId);
    }

    public List<AdminNote> getNotes(Long bookingId) {
        return adminNoteRepository
                .findAllByBookingIdOrderByCreatedAtDesc(bookingId);
    }

    public boolean canChangeBooking(Booking booking) {
        return booking.getStatus() != BookingStatus.CANCELLED
                && daysUntilDeparture(booking) > CHANGE_LOCK_DAYS_BEFORE_DEPARTURE;
    }

    public int getChangeLockDaysBeforeDeparture() {
        return CHANGE_LOCK_DAYS_BEFORE_DEPARTURE;
    }

    public void updateContact(
            Long bookingId,
            AdminBookingContactRequest request,
            String adminEmail
    ) {
        Booking booking = getEditableBooking(bookingId);
        String previousEmail = booking.getCustomer().getEmail();
        String previousPhone = booking.getCustomer().getPhone();
        String previousName =
                booking.getCustomer().getFirstName()
                        + " "
                        + booking.getCustomer().getLastName();
        String newName =
                clean(request.getFirstName())
                        + " "
                        + clean(request.getLastName());

        booking
                .getCustomer()
                .updateContactInformation(
                        clean(request.getFirstName()),
                        clean(request.getLastName()),
                        clean(request.getPhone()),
                        clean(request.getEmail())
                );

        logEvent(
                booking,
                "Kontaktuppgifter ändrade",
                "Bokningsansvarig ändrad från "
                        + previousName
                        + " till "
                        + newName
                        + ". Telefon ändrad från "
                        + previousPhone
                        + " till "
                        + clean(request.getPhone())
                        + ". E-post ändrad från "
                        + previousEmail
                        + " till "
                        + clean(request.getEmail())
                        + ".",
                adminEmail
        );
    }

    public void updateTravelerName(
            Long bookingId,
            Long travelerId,
            AdminTravelerNameRequest request,
            String adminEmail
    ) {
        Booking booking = getEditableBooking(bookingId);
        Traveler traveler = booking
                .getTravelers()
                .stream()
                .filter(currentTraveler ->
                        currentTraveler.getId().equals(travelerId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Resenären kunde inte hittas på bokningen."
                        )
                );

        String oldName =
                traveler.getFirstName()
                        + " "
                        + traveler.getLastName();
        String newName =
                clean(request.getFirstName())
                        + " "
                        + clean(request.getLastName());

        traveler.updateName(
                clean(request.getFirstName()),
                clean(request.getLastName())
        );

        logEvent(
                booking,
                "Resenärsnamn rättat",
                oldName + " ändrades till " + newName + ".",
                adminEmail
        );
    }

    public void addNote(
            Long bookingId,
            AdminNoteRequest request,
            String adminEmail
    ) {
        Booking booking = getBooking(bookingId);

        adminNoteRepository.save(new AdminNote(
                booking,
                clean(request.getNote()),
                actor(adminEmail)
        ));

        logEvent(
                booking,
                "Adminanteckning skapad",
                "En intern anteckning lades till.",
                adminEmail
        );
    }

    public void resendConfirmation(
            Long bookingId,
            String adminEmail
    ) {
        Booking booking = getBooking(bookingId);
        Invoice invoice = invoiceService
                .getInvoiceForBooking(bookingId)
                .orElseGet(() -> invoiceService.createInvoice(booking));
        EmailStatus emailStatus =
                bookingEmailService.sendBookingConfirmation(
                        booking,
                        invoice
                );

        booking.markBookingEmailStatus(emailStatus);

        logEvent(
                booking,
                "Bokningsmejl skickat igen",
                "Status efter utskick: "
                        + emailStatus.getDisplayName()
                        + ".",
                adminEmail
        );
    }

    public void regenerateInvoice(
            Long bookingId,
            String adminEmail
    ) {
        Booking booking = getBooking(bookingId);
        invoiceService.regenerateInvoice(booking);

        logEvent(
                booking,
                "Faktura genererad igen",
                "En ny faktura skapades för bokningen.",
                adminEmail
        );
    }

    public void resendCancellationCertificate(
            Long bookingId,
            String adminEmail
    ) {
        Booking booking = getBooking(bookingId);

        if (booking.getStatus() != BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Avbokningsintyg kan bara skickas för avbokade bokningar."
            );
        }

        EmailStatus emailStatus =
                bookingEmailService.sendCancellationConfirmation(booking);
        booking.markCancellationEmailStatus(emailStatus);

        logEvent(
                booking,
                "Avbokningsintyg skickat",
                "Status efter utskick: "
                        + emailStatus.getDisplayName()
                        + ".",
                adminEmail
        );
    }

    public void logEvent(
            Booking booking,
            String title,
            String description,
            String adminEmail
    ) {
        bookingEventRepository.save(new BookingEvent(
                booking,
                title,
                description,
                actor(adminEmail)
        ));
    }

    private Booking getEditableBooking(Long bookingId) {
        Booking booking = getBooking(bookingId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Det går inte att ändra en avbokad bokning."
            );
        }

        if (daysUntilDeparture(booking) <= CHANGE_LOCK_DAYS_BEFORE_DEPARTURE) {
            throw new IllegalStateException(
                    "Bokningen kan inte ändras när det är "
                            + CHANGE_LOCK_DAYS_BEFORE_DEPARTURE
                            + " dagar eller mindre kvar till avresa."
            );
        }

        return booking;
    }

    private Booking getBooking(Long bookingId) {
        return bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bokningen kunde inte hittas."
                        )
                );
    }

    private long daysUntilDeparture(Booking booking) {
        return ChronoUnit.DAYS.between(
                LocalDate.now(),
                booking.getDeparture().getDepartureDate()
        );
    }

    private String actor(String adminEmail) {
        if (adminEmail == null || adminEmail.isBlank()) {
            return "Admin";
        }

        return adminEmail;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
