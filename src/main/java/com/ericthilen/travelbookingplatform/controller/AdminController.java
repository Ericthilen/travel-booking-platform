package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.AdminBookingContactRequest;
import com.ericthilen.travelbookingplatform.dto.AdminNoteRequest;
import com.ericthilen.travelbookingplatform.dto.AdminTravelerNameRequest;
import com.ericthilen.travelbookingplatform.dto.CancellationRequest;
import com.ericthilen.travelbookingplatform.dto.DiscountCodeRequest;
import com.ericthilen.travelbookingplatform.model.BusPickupStop;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.service.AdminBookingManagementService;
import com.ericthilen.travelbookingplatform.service.AdminDashboardService;
import com.ericthilen.travelbookingplatform.service.BookingService;
import com.ericthilen.travelbookingplatform.service.DepartureService;
import com.ericthilen.travelbookingplatform.service.PaymentService;
import com.ericthilen.travelbookingplatform.service.TravelService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.TreeSet;

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
    private final TravelService travelService;
    private final DepartureService departureService;

    public AdminController(
            AdminDashboardService adminDashboardService,
            BookingService bookingService,
            PaymentService paymentService,
            AdminBookingManagementService adminBookingManagementService,
            TravelService travelService,
            DepartureService departureService
    ) {
        this.adminDashboardService = adminDashboardService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.adminBookingManagementService =
                adminBookingManagementService;
        this.travelService = travelService;
        this.departureService = departureService;
    }

    @GetMapping("/admin")
    public String showAdminDashboard(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String travelName,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate departureDate,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        model.addAttribute(
                "dashboard",
                adminDashboardService.getDashboard(
                        query,
                        travelName,
                        departureDate,
                        page
                )
        );
        addRegisterMenuModel(model);

        return "admin-dashboard";
    }

    @GetMapping("/admin/bokningar")
    public String showAdminBookings(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String travelName,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate departureDate,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        model.addAttribute(
                "dashboard",
                adminDashboardService.getDashboard(
                        query,
                        travelName,
                        departureDate,
                        page
                )
        );
        addRegisterMenuModel(model);

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

    @PostMapping("/admin/bokningar/{bookingId}/rabattkod")
    public String applyDiscountCodeAsAdmin(
            @PathVariable Long bookingId,
            @Valid DiscountCodeRequest discountCodeRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "adminDiscountError",
                    "Ange en rabattkod."
            );
            return redirectToBookingSection(
                    bookingId,
                    "admin-rabattkod"
            );
        }

        try {
            bookingService.applyAdminDiscountToBooking(
                    bookingId,
                    discountCodeRequest.getCode()
            );
            redirectAttributes.addFlashAttribute(
                    "adminDiscountMessage",
                    "Rabattkoden har lagts till."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "adminDiscountError",
                    exception.getMessage()
            );
        }

        return redirectToBookingSection(
                bookingId,
                "admin-rabattkod"
        );
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
                    "adminNoteError",
                    "Anteckningen får inte vara tom."
            );
            return redirectToBookingSection(
                    bookingId,
                    "journalanteckningar"
            );
        }

        adminBookingManagementService.addNote(
                bookingId,
                noteRequest,
                adminEmail(authentication)
        );
        redirectAttributes.addFlashAttribute(
                "adminNoteMessage",
                "Anteckningen har sparats."
        );

        return redirectToBookingSection(
                bookingId,
                "journalanteckningar"
        );
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

    private String redirectToBookingSection(
            Long bookingId,
            String sectionId
    ) {
        return redirectToBooking(bookingId) + "#" + sectionId;
    }

    private String adminEmail(Authentication authentication) {
        if (authentication == null) {
            return "Admin";
        }

        return authentication.getName();
    }

    private void addRegisterMenuModel(Model model) {
        List<Travel> allTravels = travelService.getAllTravels();

        model.addAttribute("allTravels", allTravels);
        model.addAttribute("homeDepartures", homeDepartures(allTravels));
        model.addAttribute("homeCountryGroups", homeCountryGroups(allTravels));
        model.addAttribute(
                "homeFlightDeparturePlaces",
                departurePlacesForType(allTravels, false)
        );
        model.addAttribute(
                "homeBusDeparturePlaces",
                departurePlacesForType(allTravels, true)
        );
    }

    private List<HomeDepartureCard> homeDepartures(List<Travel> travels) {
        List<HomeDepartureCard> cards = new ArrayList<>();

        for (Travel travel : travels) {
            for (Departure departure
                    : departureService.getDeparturesForTravel(travel.getId())) {
                cards.add(new HomeDepartureCard(
                        travel.getId(),
                        travel.getDestination(),
                        travel.getHotelName(),
                        travel.isBusTrip() ? "🚌" : "✈",
                        travel.getNights(),
                        departure.getDepartureAirport(),
                        departure.getDepartureDate(),
                        departure.getDepartureDate()
                                .getMonth()
                                .getDisplayName(
                                        TextStyle.FULL,
                                        Locale.forLanguageTag("sv-SE")
                                ),
                        seasonFor(departure.getDepartureDate()),
                        departure.getPricePerPerson(),
                        travel.getImageUrl()
                ));
            }
        }

        return cards
                .stream()
                .sorted(Comparator.comparing(HomeDepartureCard::departureDate))
                .toList();
    }

    private List<HomeCountryGroup> homeCountryGroups(List<Travel> travels) {
        return travels
                .stream()
                .map(Travel::getCountry)
                .filter(country -> country != null && !country.isBlank())
                .collect(Collectors.toCollection(TreeSet::new))
                .stream()
                .map(country -> new HomeCountryGroup(
                        country,
                        travels
                                .stream()
                                .filter(travel ->
                                        country.equals(travel.getCountry()))
                                .sorted(Comparator.comparing(
                                        Travel::getDestination
                                ))
                                .toList(),
                        departurePlacesForCountry(travels, country)
                ))
                .toList();
    }

    private List<HomeDeparturePlace> departurePlacesForCountry(
            List<Travel> travels,
            String country
    ) {
        List<HomeDeparturePlace> places = new ArrayList<>();

        for (Travel travel : travels) {
            if (!country.equals(travel.getCountry())) {
                continue;
            }

            if (travel.isBusTrip()) {
                for (BusPickupStop stop : travel.getPickupStops()) {
                    places.add(new HomeDeparturePlace(
                            "Buss",
                            stop.getCity()
                    ));
                }
                continue;
            }

            for (Departure departure
                    : departureService.getDeparturesForTravel(travel.getId())) {
                places.add(new HomeDeparturePlace(
                        "Flyg",
                        departure.getDepartureAirport()
                ));
            }
        }

        return uniqueDeparturePlaces(places);
    }

    private List<HomeDeparturePlace> departurePlacesForType(
            List<Travel> travels,
            boolean busTrips
    ) {
        List<HomeDeparturePlace> places = new ArrayList<>();

        for (Travel travel : travels) {
            if (travel.isBusTrip() != busTrips) {
                continue;
            }

            if (travel.isBusTrip()) {
                for (BusPickupStop stop : travel.getPickupStops()) {
                    places.add(new HomeDeparturePlace(
                            "Buss",
                            stop.getCity()
                    ));
                }
                continue;
            }

            for (Departure departure
                    : departureService.getDeparturesForTravel(travel.getId())) {
                places.add(new HomeDeparturePlace(
                        "Flyg",
                        departure.getDepartureAirport()
                ));
            }
        }

        return uniqueDeparturePlaces(places);
    }

    private List<HomeDeparturePlace> uniqueDeparturePlaces(
            List<HomeDeparturePlace> places
    ) {
        return places
                .stream()
                .filter(place -> place.name() != null
                        && !place.name().isBlank())
                .collect(Collectors.toMap(
                        place -> place.type() + place.name(),
                        place -> place,
                        (first, second) -> first
                ))
                .values()
                .stream()
                .sorted(Comparator
                        .comparing(HomeDeparturePlace::type)
                        .thenComparing(HomeDeparturePlace::name))
                .toList();
    }

    private String seasonFor(LocalDate date) {
        Month month = date.getMonth();

        return switch (month) {
            case MARCH, APRIL, MAY -> "var";
            case JUNE, JULY, AUGUST -> "sommar";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "host";
            case DECEMBER, JANUARY, FEBRUARY -> "vinter";
        };
    }

    public record HomeDepartureCard(
            Long travelId,
            String destination,
            String hotelName,
            String travelTypeIcon,
            int nights,
            String departureAirport,
            LocalDate departureDate,
            String departureMonth,
            String season,
            int price,
            String imageUrl
    ) {
    }

    public record HomeCountryGroup(
            String country,
            List<Travel> travels,
            List<HomeDeparturePlace> departurePlaces
    ) {
    }

    public record HomeDeparturePlace(
            String type,
            String name
    ) {
    }
}
