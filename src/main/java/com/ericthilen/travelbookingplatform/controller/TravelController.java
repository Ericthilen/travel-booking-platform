package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.service.TravelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class TravelController {

    private final TravelService travelService;

    public TravelController(TravelService travelService) {
        this.travelService = travelService;
    }

    @GetMapping("/resor")
    public String showTravels(Model model) {
        model.addAttribute("travels", travelService.getAllTravels());

        return "travels";
    }

    @GetMapping("/resor/{id}")
    public String showTravelDetails(
            @PathVariable Long id,
            Model model
    ) {
        Optional<Travel> travel = travelService.getTravelById(id);

        if (travel.isEmpty()) {
            return "redirect:/resor";
        }

        model.addAttribute("travel", travel.get());

        return "travel-details";
    }
}