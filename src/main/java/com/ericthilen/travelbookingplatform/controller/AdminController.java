package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.service.AdminDashboardService;
import com.ericthilen.travelbookingplatform.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final PaymentService paymentService;

    public AdminController(
            AdminDashboardService adminDashboardService,
            PaymentService paymentService
    ) {
        this.adminDashboardService = adminDashboardService;
        this.paymentService = paymentService;
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

        model.addAttribute("booking", booking.get());
        model.addAttribute(
                "payments",
                paymentService.getPaymentsForBooking(bookingId)
        );

        return "admin-booking-details";
    }
}
