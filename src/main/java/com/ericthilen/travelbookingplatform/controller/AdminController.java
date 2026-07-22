package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.AdminBookingContactRequest;
import com.ericthilen.travelbookingplatform.dto.AdminNoteRequest;
import com.ericthilen.travelbookingplatform.dto.AdminTravelerNameRequest;
import com.ericthilen.travelbookingplatform.dto.CancellationRequest;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.service.AdminBookingManagementService;
import com.ericthilen.travelbookingplatform.service.AdminDashboardService;
import com.ericthilen.travelbookingplatform.service.BookingService;
import com.ericthilen.travelbookingplatform.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class AdminController {

    private static final List<String> CANCELLATION_REASONS = List.of(
            "Personligt",
            "Sjukdom",
            "Vill inte resa",
            "Arbete eller studier",
            "Familjeskäl",
            "Ekonomiska skäl",
            "Annat"
    );

    private final AdminDashboardService adminDashboardService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final AdminBookingManagementService adminBookingManagementService;

    public AdminController(
            AdminDashboardService adminDashboardService,
            BookingService bookingService,
            PaymentService paymentService,
            AdminBookingManagementService adminBookingManagementService
    ) {
        this.adminDashboardService = adminDashboardService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.adminBookingManagementService =
                adminBookingManagementService;
    }

    @GetMapping("/admin")
    public String showAdminDashboard(
            @RequestParam(required = false) String query,
            Model model
    ) {
        model.addAttribute(
                "dashboard",
                adminDashboardService.getDashboard(query)
        );

        return "admin-dashboard";
    }

    @GetMapping("/admin/bokningar")
    public String showAdminBookings(
            @RequestParam(required = false) String query,
            Model model
    ) {
        model.addAttribute(
                "dashboard",
                adminDashboardService.getDashboard(query)
        );

        return "admin-dashboard";
    }

    @GetMapping("/admin/bokningar/{bookingId}")
    public String showAdminBookingDetails(
            @PathVariable Long bookingId,
            Model model
    ) {
        Optional<Booking> booking = paymentService.getBooking(bookingId);

        if (booking.isEmpty()) {
            return "redirect:/admin?bookingNotFound";
        }

        loadBookingDetails(
                booking.get(),
                model
        );

        if (!model.containsAttribute("contactRequest")) {
            AdminBookingContactRequest contactRequest =
                    new AdminBookingContactRequest();
            contactRequest.setFirstName(
                    booking.get().getCustomer().getFirstName()
            );
            contactRequest.setLastName(
                    booking.get().getCustomer().getLastName()
            );
            contactRequest.setEmail(
                    booking.get().getCustomer().getEmail()
            );
            contactRequest.setPhone(
                    booking.get().getCustomer().getPhone()
            );
            model.addAttribute(
                    "contactRequest",
                    contactRequest
            );
        }

        if (!model.containsAttribute("noteRequest")) {
            model.addAttribute(
                    "noteRequest",
                    new AdminNoteRequest()
            );
        }

        if (!model.containsAttribute("cancellationRequest")) {
            model.addAttribute(
                    "cancellationRequest",
                    new CancellationRequest()
            );
        }

        return "admin-booking-details";
    }

    @PostMapping("/admin/bokningar/{bookingId}/kontakt")
    public String updateContact(
            @PathVariable Long bookingId,
            @Valid
            @ModelAttribute("contactRequest")
            AdminBookingContactRequest contactRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "adminBookingError",
                    "Kontrollera namn, telefonnummer och e-post."
            );
            return redirectToBooking(bookingId);
        }

        try {
            adminBookingManagementService.updateContact(
                    bookingId,
                    contactRequest,
                    adminEmail(authentication)
            );
            redirectAttributes.addFlashAttribute(
                    "adminBookingMessage",
                    "Kontaktuppgifterna har uppdaterats."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "adminBookingError",
                    exception.getMessage()
            );
        }

        return redirectToBooking(bookingId);
    }

    @PostMapping("/admin/bokningar/{bookingId}/avboka")
    public String cancelBookingAsAdmin(
            @PathVariable Long bookingId,
            @Valid
            @ModelAttribute("cancellationRequest")
            CancellationRequest cancellationRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "adminBookingError",
                    "Välj avbokningsorsak och bekräfta avbokningen."
            );
            return redirectToBooking(bookingId);
        }

        try {
            bookingService.cancelBookingForAdmin(
                    bookingId,
                    adminEmail(authentication),
                    cancellationRequest.getCancellationReason()
            );
            redirectAttributes.addFlashAttribute(
                    "adminBookingMessage",
                    "Bokningen har avbokats."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "adminBookingError",
                    exception.getMessage()
            );
        }

        return redirectToBooking(bookingId);
    }

    @PostMapping("/admin/bokningar/{bookingId}/skicka-avbokningsintyg")
    public String resendCancellationCertificate(
            @PathVariable Long bookingId,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        try {
            adminBookingManagementService.resendCancellationCertificate(
                    bookingId,
                    adminEmail(authentication)
            );
            redirectAttributes.addFlashAttribute(
                    "adminBookingMessage",
                    "Avbokningsintyget har skickats."
            );
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "adminBookingError",
                    exception.getMessage()
            );
        }

        return redirectToBooking(bookingId);
    }

    @PostMapping("/admin/bokningar/{bookingId}/resenarer/{travelerId}")
    public String updateTravelerName(
            @PathVariable Long bookingId,
            @PathVariable Long travelerId,
            @Valid
            @ModelAttribute AdminTravelerNameRequest travelerRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "adminBookingError",
                    "Resenärens namn måste fyllas i."
            );
            return redirectToBooking(bookingId);
        }

        try {
            adminBookingManagementService.updateTravelerName(
                    bookingId,
                    travelerId,
                    travelerRequest,
                    adminEmail(authentication)
            );
            redirectAttributes.addFlashAttribute(
                    "adminBookingMessage",
                    "Resenärens namn har uppdaterats."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "adminBookingError",
                    exception.getMessage()
            );
        }

        return redirectToBooking(bookingId);
    }

    @PostMapping("/admin/bokningar/{bookingId}/anteckningar")
    public String addNote(
            @PathVariable Long bookingId,
            @Valid
            @ModelAttribute("noteRequest")
            AdminNoteRequest noteRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "adminBookingError",
                    "Anteckningen får inte vara tom."
            );
            return redirectToBooking(bookingId);
        }

        adminBookingManagementService.addNote(
                bookingId,
                noteRequest,
                adminEmail(authentication)
        );
        redirectAttributes.addFlashAttribute(
                "adminBookingMessage",
                "Anteckningen har sparats."
        );

        return redirectToBooking(bookingId);
    }

    @PostMapping("/admin/bokningar/{bookingId}/skicka-bekraftelse")
    public String resendConfirmation(
            @PathVariable Long bookingId,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        adminBookingManagementService.resendConfirmation(
                bookingId,
                adminEmail(authentication)
        );
        redirectAttributes.addFlashAttribute(
                "adminBookingMessage",
                "Bokningsbekräftelsen har skickats igen."
        );

        return redirectToBooking(bookingId);
    }

    @PostMapping("/admin/bokningar/{bookingId}/generera-faktura")
    public String regenerateInvoice(
            @PathVariable Long bookingId,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        adminBookingManagementService.regenerateInvoice(
                bookingId,
                adminEmail(authentication)
        );
        redirectAttributes.addFlashAttribute(
                "adminBookingMessage",
                "Fakturan har genererats igen."
        );

        return redirectToBooking(bookingId);
    }

    private void loadBookingDetails(
            Booking booking,
            Model model
    ) {
        model.addAttribute("booking", booking);
        model.addAttribute(
                "payments",
                paymentService.getPaymentsForBooking(booking.getId())
        );
        model.addAttribute(
                "events",
                adminBookingManagementService.getEvents(booking.getId())
        );
        model.addAttribute(
                "notes",
                adminBookingManagementService.getNotes(booking.getId())
        );
        model.addAttribute(
                "canChangeBooking",
                adminBookingManagementService.canChangeBooking(booking)
        );
        model.addAttribute(
                "changeLockDays",
                adminBookingManagementService
                        .getChangeLockDaysBeforeDeparture()
        );
        model.addAttribute(
                "cancellationReasons",
                CANCELLATION_REASONS
        );
        if (booking.getStatus().name().equals("CONFIRMED")) {
            model.addAttribute(
                    "cancellationSummary",
                    bookingService.calculateCancellation(booking)
            );
        }
    }

    private String redirectToBooking(Long bookingId) {
        return "redirect:/admin/bokningar/" + bookingId;
    }

    private String adminEmail(Authentication authentication) {
        if (authentication == null) {
            return "Admin";
        }

        return authentication.getName();
    }
}
