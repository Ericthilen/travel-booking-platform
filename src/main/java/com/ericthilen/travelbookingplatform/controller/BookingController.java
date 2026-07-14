package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.CancellationRequest;
import com.ericthilen.travelbookingplatform.dto.CancellationSummary;
import com.ericthilen.travelbookingplatform.dto.CustomerPaymentRequest;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.BookingStatus;
import com.ericthilen.travelbookingplatform.service.BookingService;
import com.ericthilen.travelbookingplatform.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    public BookingController(
            BookingService bookingService,
            PaymentService paymentService
    ) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
    }

    @GetMapping("/mina-bokningar")
    public String showMyBookings(
            Authentication authentication,
            Model model
    ) {
        model.addAttribute(
                "bookings",
                bookingService.getBookingsForUser(
                        authentication.getName()
                )
        );

        return "my-bookings";
    }

    @GetMapping("/mina-bokningar/{bookingId}")
    public String showBookingDetails(
            @PathVariable Long bookingId,
            Authentication authentication,
            Model model
    ) {
        Optional<Booking> booking =
                bookingService.getBookingForUser(
                        bookingId,
                        authentication.getName()
                );

        if (booking.isEmpty()) {
            return "redirect:/mina-bokningar?notFound";
        }

        model.addAttribute(
                "booking",
                booking.get()
        );

        model.addAttribute(
                "payments",
                paymentService.getPaymentsForBooking(
                        bookingId
                )
        );

        CustomerPaymentRequest paymentRequest = new CustomerPaymentRequest();
        paymentRequest.setAmount(booking.get().getRemainingAmount());
        model.addAttribute("paymentRequest", paymentRequest);

        return "booking-details";
    }

    @GetMapping("/mina-bokningar/{bookingId}/avboka")
    public String showCancellationPage(
            @PathVariable Long bookingId,
            Authentication authentication,
            Model model
    ) {
        Optional<Booking> booking =
                bookingService.getBookingForUser(
                        bookingId,
                        authentication.getName()
                );

        if (booking.isEmpty()) {
            return "redirect:/mina-bokningar?notFound";
        }

        if (booking.get().getStatus()
                == BookingStatus.CANCELLED) {

            return "redirect:/mina-bokningar/"
                    + bookingId
                    + "?alreadyCancelled";
        }

        CancellationSummary cancellationSummary =
                bookingService.calculateCancellation(
                        booking.get()
                );

        model.addAttribute(
                "booking",
                booking.get()
        );

        model.addAttribute(
                "cancellationSummary",
                cancellationSummary
        );

        model.addAttribute(
                "cancellationRequest",
                new CancellationRequest()
        );

        return "booking-cancellation";
    }

    @PostMapping("/mina-bokningar/{bookingId}/avboka")
    public String cancelBooking(
            @PathVariable Long bookingId,
            @Valid CancellationRequest cancellationRequest,
            BindingResult bindingResult,
            Authentication authentication,
            Model model
    ) {
        Optional<Booking> booking =
                bookingService.getBookingForUser(
                        bookingId,
                        authentication.getName()
                );

        if (booking.isEmpty()) {
            return "redirect:/mina-bokningar?notFound";
        }

        if (booking.get().getStatus()
                == BookingStatus.CANCELLED) {

            return "redirect:/mina-bokningar/"
                    + bookingId
                    + "?alreadyCancelled";
        }

        CancellationSummary cancellationSummary =
                bookingService.calculateCancellation(
                        booking.get()
                );

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "booking",
                    booking.get()
            );

            model.addAttribute(
                    "cancellationSummary",
                    cancellationSummary
            );

            return "booking-cancellation";
        }

        bookingService.cancelBooking(
                bookingId,
                authentication.getName()
        );

        return "redirect:/mina-bokningar/"
                + bookingId
                + "?cancelled";
    }
}