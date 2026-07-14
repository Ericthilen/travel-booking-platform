package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.PaymentRequest;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.PaymentMethod;
import com.ericthilen.travelbookingplatform.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;

@Controller
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(
            PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @GetMapping("/admin/bokningar/{bookingId}/betalningar")
    public String showPaymentPage(
            @PathVariable Long bookingId,
            Model model
    ) {
        Optional<Booking> booking =
                paymentService.getBooking(bookingId);

        if (booking.isEmpty()) {
            return "redirect:/admin?bookingNotFound";
        }

        if (!model.containsAttribute("paymentRequest")) {
            model.addAttribute(
                    "paymentRequest",
                    createDefaultRequest(booking.get())
            );
        }

        loadPage(
                booking.get(),
                model
        );

        return "admin-booking-payment";
    }

    @PostMapping("/admin/bokningar/{bookingId}/betalningar")
    public String registerPayment(
            @PathVariable Long bookingId,
            @Valid
            @ModelAttribute("paymentRequest")
            PaymentRequest paymentRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<Booking> booking =
                paymentService.getBooking(bookingId);

        if (booking.isEmpty()) {
            return "redirect:/admin?bookingNotFound";
        }

        if (bindingResult.hasErrors()) {
            loadPage(
                    booking.get(),
                    model
            );

            return "admin-booking-payment";
        }

        try {
            paymentService.registerPayment(
                    bookingId,
                    paymentRequest
            );
        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {
            bindingResult.reject(
                    "payment.registration.failed",
                    exception.getMessage()
            );

            loadPage(
                    booking.get(),
                    model
            );

            return "admin-booking-payment";
        }

        redirectAttributes.addAttribute(
                "registered",
                true
        );

        return "redirect:/admin/bokningar/"
                + bookingId
                + "/betalningar";
    }

    private PaymentRequest createDefaultRequest(
            Booking booking
    ) {
        PaymentRequest paymentRequest =
                new PaymentRequest();

        int remainingToPay = Math.max(
                0,
                booking.getTotalPrice()
                        - booking.getPaidAmount()
        );

        if (booking.getPaidAmount() == 0
                && booking.getDepositAmount() > 0) {

            paymentRequest.setAmount(
                    Math.min(
                            booking.getDepositAmount(),
                            remainingToPay
                    )
            );
        } else {
            paymentRequest.setAmount(
                    remainingToPay
            );
        }

        paymentRequest.setPaymentDate(
                LocalDate.now()
        );

        paymentRequest.setPaymentMethod(
                PaymentMethod.BANK_TRANSFER
        );

        return paymentRequest;
    }

    private void loadPage(
            Booking booking,
            Model model
    ) {
        model.addAttribute(
                "booking",
                booking
        );

        model.addAttribute(
                "payments",
                paymentService.getPaymentsForBooking(
                        booking.getId()
                )
        );

        model.addAttribute(
                "paymentMethods",
                PaymentMethod.values()
        );

        model.addAttribute(
                "remainingToPay",
                Math.max(
                        0,
                        booking.getTotalPrice()
                                - booking.getPaidAmount()
                )
        );
    }
}