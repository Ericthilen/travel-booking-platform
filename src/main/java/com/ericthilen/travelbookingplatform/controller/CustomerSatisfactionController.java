package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.service.CustomerSatisfactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomerSatisfactionController {

    private final CustomerSatisfactionService satisfactionService;

    public CustomerSatisfactionController(
            CustomerSatisfactionService satisfactionService
    ) {
        this.satisfactionService = satisfactionService;
    }

    @GetMapping("/kundnojdhet/{surveyToken}")
    public String rate(
            @PathVariable String surveyToken,
            @RequestParam int rating,
            Model model
    ) {
        model.addAttribute(
                "review",
                satisfactionService.submitRating(surveyToken, rating)
        );

        return "customer-satisfaction-thanks";
    }
}
