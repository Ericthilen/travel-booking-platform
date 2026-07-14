package com.ericthilen.travelbookingplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin")
    public String adminRedirect() {
        return "redirect:/";
    }

    @GetMapping("/admin/bokningar")
    public String adminBookingsRedirect() {
        return "redirect:/";
    }
}
