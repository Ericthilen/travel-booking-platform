package com.ericthilen.travelbookingplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BookingController {

    @GetMapping("/mina-bokningar")
    public String showMyBookings() {
        return "my-bookings";
    }
}