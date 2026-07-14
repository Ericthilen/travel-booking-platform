package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.CustomerPaymentRequest;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class CustomerPaymentController {

    private final PaymentService paymentService;

    public CustomerPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/mina-bokningar/{bookingId}/betala")
    public String handlePayment(
            @PathVariable Long bookingId,
            @Valid @ModelAttribute("paymentRequest") CustomerPaymentRequest paymentRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        Optional<Booking> booking = paymentService.getBooking(bookingId);
        if (booking.isEmpty()) {
            return "redirect:/mina-bokningar?error";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("paymentError", "Betalningen kunde inte genomföras. Kontrollera uppgifterna.");
            return "redirect:/mina-bokningar/" + bookingId;
        }

        try {
            paymentService.processCustomerPayment(
                    bookingId,
                    paymentRequest.getAmount(),
                    paymentRequest.getPaymentMethod()
            );
            redirectAttributes.addFlashAttribute("paymentSuccess", "Betalningen genomfördes utan problem!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("paymentError", "Ett fel uppstod: " + e.getMessage());
        }

        return "redirect:/mina-bokningar/" + bookingId;
    }
}
