package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.BookingLookupRequest;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
public class BookingLookupController {

    private final BookingService bookingService;

    public BookingLookupController(
            BookingService bookingService
    ) {
        this.bookingService = bookingService;
    }

    @GetMapping("/hitta-bokning")
    public String showBookingLookup(Model model) {
        model.addAttribute(
                "bookingLookupRequest",
                new BookingLookupRequest()
        );

        return "booking-lookup";
    }

    @PostMapping("/hitta-bokning")
    public String findBooking(
            @Valid BookingLookupRequest bookingLookupRequest,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "booking-lookup";
        }

        Optional<Booking> booking =
                bookingService.findBooking(
                        bookingLookupRequest
                );

        if (booking.isEmpty()) {
            model.addAttribute(
                    "lookupError",
                    "Vi kunde inte hitta någon bokning med de angivna uppgifterna."
            );

            return "booking-lookup";
        }

        model.addAttribute("booking", booking.get());

        return "booking-lookup-result";
    }
}