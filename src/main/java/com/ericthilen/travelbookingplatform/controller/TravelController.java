package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.service.DepartureService;
import com.ericthilen.travelbookingplatform.service.TravelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class TravelController {

    private final TravelService travelService;
    private final DepartureService departureService;

    public TravelController(
            TravelService travelService,
            DepartureService departureService
    ) {
        this.travelService = travelService;
        this.departureService = departureService;
    }

    @GetMapping("/resor")
    public String showTravels(Model model) {
        model.addAttribute(
                "travels",
                travelService.getAllTravels()
        );

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

    @GetMapping("/resor/{id}/avgangar")
    public String showTravelDepartures(
            @PathVariable Long id,
            Model model
    ) {
        Optional<Travel> travel = travelService.getTravelById(id);

        if (travel.isEmpty()) {
            return "redirect:/resor";
        }

        model.addAttribute("travel", travel.get());
        model.addAttribute(
                "departures",
                departureService.getDeparturesForTravel(id)
        );

        return "travel-departures";
    }
}