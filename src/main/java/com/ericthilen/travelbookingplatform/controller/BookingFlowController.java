package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.BookingConfirmationRequest;
import com.ericthilen.travelbookingplatform.dto.BookingDetailsRequest;
import com.ericthilen.travelbookingplatform.dto.BookingSelectionRequest;
import com.ericthilen.travelbookingplatform.dto.BookingSession;
import com.ericthilen.travelbookingplatform.dto.BookingSummary;
import com.ericthilen.travelbookingplatform.dto.TravelerRequest;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.model.DiscoverySource;
import com.ericthilen.travelbookingplatform.model.RoomType;
import com.ericthilen.travelbookingplatform.service.BookingService;
import com.ericthilen.travelbookingplatform.service.DepartureService;
import com.ericthilen.travelbookingplatform.service.RoomTypeService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class BookingFlowController {

    private static final String BOOKING_SESSION_KEY =
            "bookingSession";

    private final DepartureService departureService;
    private final RoomTypeService roomTypeService;
    private final BookingService bookingService;

    public BookingFlowController(
            DepartureService departureService,
            RoomTypeService roomTypeService,
            BookingService bookingService
    ) {
        this.departureService = departureService;
        this.roomTypeService = roomTypeService;
        this.bookingService = bookingService;
    }

    @GetMapping("/bokning/{departureId}/resenarer")
    public String showTravelerSelection(
            @PathVariable Long departureId,
            Model model
    ) {
        Optional<Departure> departure =
                departureService.getDepartureById(departureId);

        if (departure.isEmpty()) {
            return "redirect:/resor";
        }

        BookingSelectionRequest request =
                new BookingSelectionRequest();

        request.setRoomOccupancies(List.of(2));

        model.addAttribute("departure", departure.get());
        model.addAttribute("bookingSelectionRequest", request);

        return "booking-travelers";
    }

    @PostMapping("/bokning/{departureId}/resenarer")
    public String saveTravelerSelection(
            @PathVariable Long departureId,
            @Valid BookingSelectionRequest bookingSelectionRequest,
            BindingResult bindingResult,
            Model model,
            HttpSession httpSession
    ) {
        Optional<Departure> departure =
                departureService.getDepartureById(departureId);

        if (departure.isEmpty()) {
            return "redirect:/resor";
        }

        validateTravelerSelection(
                bookingSelectionRequest,
                bindingResult
        );

        if (bookingSelectionRequest.getNumberOfTravelers()
                > departure.get().getAvailableSeats()) {

            bindingResult.rejectValue(
                    "numberOfTravelers",
                    "notEnoughSeats",
                    "Det finns inte tillräckligt många platser kvar."
            );
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("departure", departure.get());

            return "booking-travelers";
        }

        BookingSession bookingSession =
                new BookingSession();

        bookingSession.setDepartureId(departureId);
        bookingSession.setNumberOfTravelers(
                bookingSelectionRequest.getNumberOfTravelers()
        );
        bookingSession.setNumberOfRooms(
                bookingSelectionRequest.getNumberOfRooms()
        );
        bookingSession.setRoomOccupancies(
                bookingSelectionRequest.getRoomOccupancies()
        );

        httpSession.setAttribute(
                BOOKING_SESSION_KEY,
                bookingSession
        );

        return "redirect:/bokning/rumstyp";
    }

    @GetMapping("/bokning/rumstyp")
    public String showRoomTypeSelection(
            HttpSession httpSession,
            Model model
    ) {
        BookingSession bookingSession =
                getBookingSession(httpSession);

        if (bookingSession == null) {
            return "redirect:/resor";
        }

        Optional<Departure> departure =
                departureService.getDepartureById(
                        bookingSession.getDepartureId()
                );

        if (departure.isEmpty()) {
            return "redirect:/resor";
        }

        return loadRoomTypePage(
                departure.get(),
                bookingSession,
                model
        );
    }

    @PostMapping("/bokning/rumstyp")
    public String saveRoomTypeSelection(
            @RequestParam Long roomTypeId,
            HttpSession httpSession,
            Model model
    ) {
        BookingSession bookingSession =
                getBookingSession(httpSession);

        if (bookingSession == null) {
            return "redirect:/resor";
        }

        Optional<Departure> departure =
                departureService.getDepartureById(
                        bookingSession.getDepartureId()
                );

        Optional<RoomType> roomType =
                roomTypeService.getRoomTypeById(roomTypeId);

        if (departure.isEmpty() || roomType.isEmpty()) {
            return "redirect:/resor";
        }

        if (!roomType.get().getTravel().getId()
                .equals(departure.get().getTravel().getId())) {

            model.addAttribute(
                    "errorMessage",
                    "Den valda rumstypen tillhör inte denna resa."
            );

            return loadRoomTypePage(
                    departure.get(),
                    bookingSession,
                    model
            );
        }

        if (!roomTypeCanBeBooked(
                roomType.get(),
                bookingSession
        )) {
            model.addAttribute(
                    "errorMessage",
                    "Rumstypen kan inte bokas med den valda fördelningen."
            );

            return loadRoomTypePage(
                    departure.get(),
                    bookingSession,
                    model
            );
        }

        bookingSession.setRoomTypeId(roomTypeId);

        httpSession.setAttribute(
                BOOKING_SESSION_KEY,
                bookingSession
        );

        return "redirect:/bokning/kunduppgifter";
    }

    @GetMapping("/bokning/kunduppgifter")
    public String showContactInformation(
            HttpSession httpSession,
            Model model
    ) {
        BookingSession bookingSession =
                getBookingSession(httpSession);

        if (bookingSession == null
                || bookingSession.getRoomTypeId() == null) {
            return "redirect:/resor";
        }

        model.addAttribute("bookingSummary", bookingService.calculateBookingSummary(bookingSession));
        model.addAttribute("bookingSession", bookingSession);
        model.addAttribute(
                "bookingDetailsRequest",
                createBookingDetailsRequest(bookingSession)
        );

        return "booking-contact";
    }

    @PostMapping("/bokning/kunduppgifter")
    public String saveContactInformation(
            @Valid BookingDetailsRequest bookingDetailsRequest,
            BindingResult bindingResult,
            HttpSession httpSession,
            Model model
    ) {
        BookingSession bookingSession =
                getBookingSession(httpSession);

        if (bookingSession == null
                || bookingSession.getRoomTypeId() == null) {
            return "redirect:/resor";
        }

        Optional<Departure> departure =
                departureService.getDepartureById(
                        bookingSession.getDepartureId()
                );

        Optional<RoomType> roomType =
                roomTypeService.getRoomTypeById(
                        bookingSession.getRoomTypeId()
                );

        if (departure.isEmpty() || roomType.isEmpty()) {
            return "redirect:/resor";
        }

        if (bookingDetailsRequest.getTravelers() == null
                || bookingDetailsRequest.getTravelers().size()
                != bookingSession.getNumberOfTravelers()) {

            bindingResult.reject(
                    "invalidTravelerCount",
                    "Antalet resenärer stämmer inte med bokningen."
            );
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("bookingSummary", bookingService.calculateBookingSummary(bookingSession));
            model.addAttribute("bookingSession", bookingSession);

            return "booking-contact";
        }

        saveBookingDetails(
                bookingSession,
                bookingDetailsRequest
        );

        httpSession.setAttribute(
                BOOKING_SESSION_KEY,
                bookingSession
        );

        return "redirect:/bokning/sammanfattning";
    }

    @GetMapping("/bokning/sammanfattning")
    public String showBookingReview(
            HttpSession httpSession,
            Model model
    ) {
        BookingSession bookingSession =
                getCompleteBookingSession(httpSession);

        if (bookingSession == null) {
            return "redirect:/resor";
        }

        BookingConfirmationRequest confirmationRequest =
                new BookingConfirmationRequest();

        return loadReviewPage(
                bookingSession,
                confirmationRequest,
                model
        );
    }

    @PostMapping("/bokning/bekrafta")
    public String confirmBooking(
            @Valid BookingConfirmationRequest confirmationRequest,
            BindingResult bindingResult,
            HttpSession httpSession,
            Authentication authentication,
            Model model
    ) {
        BookingSession bookingSession =
                getCompleteBookingSession(httpSession);

        if (bookingSession == null) {
            return "redirect:/resor";
        }

        if (bindingResult.hasErrors()) {
            return loadReviewPage(
                    bookingSession,
                    confirmationRequest,
                    model
            );
        }

        String authenticatedEmail = null;

        if (authentication != null
                && authentication.isAuthenticated()) {
            authenticatedEmail = authentication.getName();
        }

        try {
            Booking booking = bookingService.createBooking(
                    bookingSession,
                    confirmationRequest,
                    authenticatedEmail
            );

            httpSession.removeAttribute(BOOKING_SESSION_KEY);

            model.addAttribute("booking", booking);

            return "booking-confirmation";
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {

            model.addAttribute(
                    "bookingError",
                    exception.getMessage()
            );

            return loadReviewPage(
                    bookingSession,
                    confirmationRequest,
                    model
            );
        }
    }

    private String loadReviewPage(
            BookingSession bookingSession,
            BookingConfirmationRequest confirmationRequest,
            Model model
    ) {
        BookingSummary summary = bookingService.calculateBookingSummary(bookingSession);
        if (summary == null) {
            return "redirect:/resor";
        }

        model.addAttribute("bookingSummary", summary);
        model.addAttribute("bookingSession", bookingSession);
        model.addAttribute(
                "discoverySources",
                DiscoverySource.values()
        );
        model.addAttribute(
                "bookingConfirmationRequest",
                confirmationRequest
        );

        return "booking-review";
    }

    private BookingDetailsRequest createBookingDetailsRequest(
            BookingSession bookingSession
    ) {
        BookingDetailsRequest request =
                new BookingDetailsRequest();

        request.setResponsiblePersonalNumber(
                bookingSession.getResponsiblePersonalNumber()
        );
        request.setResponsibleFirstName(
                bookingSession.getResponsibleFirstName()
        );
        request.setResponsibleLastName(
                bookingSession.getResponsibleLastName()
        );
        request.setResponsiblePhone(
                bookingSession.getResponsiblePhone()
        );
        request.setResponsibleEmail(
                bookingSession.getResponsibleEmail()
        );

        List<TravelerRequest> travelers =
                new ArrayList<>();

        if (bookingSession.getTravelers().size()
                == bookingSession.getNumberOfTravelers()) {
            travelers.addAll(bookingSession.getTravelers());
        } else {
            for (
                    int index = 0;
                    index < bookingSession.getNumberOfTravelers();
                    index++
            ) {
                travelers.add(new TravelerRequest());
            }
        }

        request.setTravelers(travelers);

        return request;
    }

    private void saveBookingDetails(
            BookingSession bookingSession,
            BookingDetailsRequest request
    ) {
        bookingSession.setResponsiblePersonalNumber(
                request.getResponsiblePersonalNumber().trim()
        );
        bookingSession.setResponsibleFirstName(
                request.getResponsibleFirstName().trim()
        );
        bookingSession.setResponsibleLastName(
                request.getResponsibleLastName().trim()
        );
        bookingSession.setResponsiblePhone(
                request.getResponsiblePhone().trim()
        );
        bookingSession.setResponsibleEmail(
                request.getResponsibleEmail()
                        .trim()
                        .toLowerCase()
        );

        for (TravelerRequest traveler : request.getTravelers()) {
            traveler.setPersonalNumber(
                    traveler.getPersonalNumber().trim()
            );
            traveler.setFirstName(
                    traveler.getFirstName().trim()
            );
            traveler.setLastName(
                    traveler.getLastName().trim()
            );
        }

        bookingSession.setTravelers(request.getTravelers());
    }

    private void validateTravelerSelection(
            BookingSelectionRequest request,
            BindingResult bindingResult
    ) {
        List<Integer> roomOccupancies =
                request.getRoomOccupancies();

        if (roomOccupancies == null) {
            bindingResult.rejectValue(
                    "roomOccupancies",
                    "missingOccupancies",
                    "Du måste fördela resenärerna mellan rummen."
            );

            return;
        }

        if (roomOccupancies.size()
                != request.getNumberOfRooms()) {
            bindingResult.rejectValue(
                    "roomOccupancies",
                    "invalidRoomCount",
                    "Antalet rumsfördelningar stämmer inte."
            );

            return;
        }

        boolean containsEmptyRoom =
                roomOccupancies.stream()
                        .anyMatch(occupancy ->
                                occupancy == null || occupancy < 1
                        );

        if (containsEmptyRoom) {
            bindingResult.rejectValue(
                    "roomOccupancies",
                    "emptyRoom",
                    "Varje rum måste ha minst en resenär."
            );
        }

        int totalOccupancy =
                roomOccupancies.stream()
                        .filter(occupancy -> occupancy != null)
                        .mapToInt(Integer::intValue)
                        .sum();

        if (totalOccupancy != request.getNumberOfTravelers()) {
            bindingResult.rejectValue(
                    "roomOccupancies",
                    "invalidOccupancyTotal",
                    "Rumsfördelningen måste motsvara antalet resenärer."
            );
        }
    }

    private List<RoomType> findAvailableRoomTypes(
            Departure departure,
            BookingSession bookingSession
    ) {
        List<RoomType> availableRoomTypes =
                new ArrayList<>();

        for (RoomType roomType
                : roomTypeService.getRoomTypesForTravel(
                departure.getTravel().getId()
        )) {

            if (roomTypeCanBeBooked(
                    roomType,
                    bookingSession
            )) {
                availableRoomTypes.add(roomType);
            }
        }

        return availableRoomTypes;
    }

    private boolean roomTypeCanBeBooked(
            RoomType roomType,
            BookingSession bookingSession
    ) {
        if (roomType.getAvailableRooms()
                < bookingSession.getNumberOfRooms()) {
            return false;
        }

        for (Integer occupancy
                : bookingSession.getRoomOccupancies()) {

            if (occupancy == null
                    || occupancy > roomType.getMaxGuests()) {
                return false;
            }
        }

        return true;
    }

    private String loadRoomTypePage(
            Departure departure,
            BookingSession bookingSession,
            Model model
    ) {
        model.addAttribute("departure", departure);
        model.addAttribute("bookingSession", bookingSession);
        model.addAttribute("bookingSummary", bookingService.calculateBookingSummary(bookingSession));
        model.addAttribute(
                "roomTypes",
                findAvailableRoomTypes(
                        departure,
                        bookingSession
                )
        );

        return "booking-room-type";
    }

    private BookingSession getCompleteBookingSession(
            HttpSession httpSession
    ) {
        BookingSession bookingSession =
                getBookingSession(httpSession);

        if (bookingSession == null
                || bookingSession.getDepartureId() == null
                || bookingSession.getRoomTypeId() == null
                || bookingSession.getTravelers().isEmpty()) {
            return null;
        }

        return bookingSession;
    }

    private BookingSession getBookingSession(
            HttpSession httpSession
    ) {
        Object value = httpSession.getAttribute(
                BOOKING_SESSION_KEY
        );

        if (value instanceof BookingSession bookingSession) {
            return bookingSession;
        }

        return null;
    }
}