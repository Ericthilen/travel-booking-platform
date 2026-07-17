package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.service.TravelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final TravelService travelService;

    public HomeController(TravelService travelService) {
        this.travelService = travelService;
    }

    @GetMapping("/")
    public String showHomepage(Model model) {
        List<Travel> popularTravels =
                travelService.getAllTravels()
                        .stream()
                        .limit(6)
                        .toList();

        model.addAttribute(
                "popularTravels",
                popularTravels
        );

        return "index";
    }
}
