package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.service.DepartureService;
import com.ericthilen.travelbookingplatform.service.TravelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final TravelService travelService;
    private final DepartureService departureService;

    public HomeController(
            TravelService travelService,
            DepartureService departureService
    ) {
        this.travelService = travelService;
        this.departureService = departureService;
    }

    @GetMapping("/")
    public String showHomepage(Model model) {
        List<Travel> allTravels = travelService.getAllTravels();
        List<Travel> popularTravels =
                allTravels
                        .stream()
                        .limit(6)
                        .toList();

        model.addAttribute(
                "popularTravels",
                popularTravels
        );
        model.addAttribute(
                "allTravels",
                allTravels
        );
        model.addAttribute(
                "departureCounts",
                departureCounts(allTravels)
        );

        return "index";
    }

    private Map<Long, Integer> departureCounts(List<Travel> travels) {
        return travels
                .stream()
                .collect(Collectors.toMap(
                        Travel::getId,
                        travel -> departureService
                                .getDeparturesForTravel(travel.getId())
                                .size()
                ));
    }
}
