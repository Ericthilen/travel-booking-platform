package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.service.DepartureService;
import com.ericthilen.travelbookingplatform.service.TravelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
    public String showTravels(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer hotelStars,
            @RequestParam(required = false) Integer nights,
            @RequestParam(required = false) String mealType,
            @RequestParam(required = false) Boolean pool,
            @RequestParam(required = false) Boolean beach,
            @RequestParam(required = false) Boolean family,
            @RequestParam(required = false) String departureAirport,
            @RequestParam(required = false) LocalDate departureDate,
            @RequestParam(required = false) String sort,
            Model model
    ) {
        List<Travel> allTravels = travelService.getAllTravels();
        List<Travel> travels = allTravels
                .stream()
                .filter(travel -> matchesText(
                        travel,
                        destination
                ))
                .filter(travel -> matchesValue(
                        travel.getCountry(),
                        country
                ))
                .filter(travel -> maxPrice == null
                        || travel.getPrice() <= maxPrice)
                .filter(travel -> hotelStars == null
                        || travel.getHotelStars() >= hotelStars)
                .filter(travel -> nights == null
                        || travel.getNights() == nights)
                .filter(travel -> matchesValue(
                        travel.getMealType(),
                        mealType
                ))
                .filter(travel -> !Boolean.TRUE.equals(pool)
                        || hasKeyword(travel, "pool"))
                .filter(travel -> !Boolean.TRUE.equals(beach)
                        || hasKeyword(travel, "strand"))
                .filter(travel -> !Boolean.TRUE.equals(family)
                        || hasKeyword(travel, "familj")
                        || hasKeyword(travel, "barn"))
                .filter(travel -> matchesValue(
                        travel.getDepartureAirport(),
                        departureAirport
                ))
                .filter(travel -> departureDate == null
                        || hasDepartureFrom(
                        travel,
                        departureDate
                ))
                .toList();

        travels = sortTravels(
                travels,
                sort
        );

        model.addAttribute(
                "travels",
                travels
        );

        model.addAttribute(
                "destinations",
                uniqueValues(
                        allTravels,
                        Travel::getDestination
                )
        );
        model.addAttribute(
                "countries",
                uniqueValues(
                        allTravels,
                        Travel::getCountry
                )
        );
        model.addAttribute(
                "mealTypes",
                uniqueValues(
                        allTravels,
                        Travel::getMealType
                )
        );
        model.addAttribute(
                "departureAirports",
                uniqueValues(
                        allTravels,
                        Travel::getDepartureAirport
                )
        );
        model.addAttribute(
                "nightOptions",
                allTravels
                        .stream()
                        .map(Travel::getNights)
                        .distinct()
                        .sorted()
                        .toList()
        );
        model.addAttribute("destination", clean(destination));
        model.addAttribute("country", country);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("hotelStars", hotelStars);
        model.addAttribute("nights", nights);
        model.addAttribute("mealType", mealType);
        model.addAttribute("pool", Boolean.TRUE.equals(pool));
        model.addAttribute("beach", Boolean.TRUE.equals(beach));
        model.addAttribute("family", Boolean.TRUE.equals(family));
        model.addAttribute("departureAirport", departureAirport);
        model.addAttribute("departureDate", departureDate);
        model.addAttribute("sort", clean(sort));

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

    private boolean matchesText(
            Travel travel,
            String query
    ) {
        String cleanedQuery = clean(query);

        if (cleanedQuery.isBlank()) {
            return true;
        }

        String searchableText = (
                travel.getDestination()
                        + " "
                        + travel.getCountry()
                        + " "
                        + travel.getHotelName()
                        + " "
                        + travel.getDescription()
        ).toLowerCase();

        return searchableText.contains(
                cleanedQuery.toLowerCase()
        );
    }

    private boolean matchesValue(
            String actualValue,
            String selectedValue
    ) {
        if (selectedValue == null
                || selectedValue.isBlank()) {
            return true;
        }

        return actualValue != null
                && actualValue.equalsIgnoreCase(
                selectedValue.trim()
        );
    }

    private boolean hasKeyword(
            Travel travel,
            String keyword
    ) {
        String text = (
                travel.getDescription()
                        + " "
                        + String.join(
                        " ",
                        travel.getFacilities()
                )
        ).toLowerCase();

        return text.contains(keyword.toLowerCase());
    }

    private boolean hasDepartureFrom(
            Travel travel,
            LocalDate departureDate
    ) {
        return departureService
                .getDeparturesForTravel(travel.getId())
                .stream()
                .anyMatch(departure ->
                        !departure.getDepartureDate()
                                .isBefore(departureDate)
                );
    }

    private List<String> uniqueValues(
            List<Travel> travels,
            java.util.function.Function<Travel, String> valueGetter
    ) {
        return travels
                .stream()
                .map(valueGetter)
                .filter(value -> value != null
                        && !value.isBlank())
                .collect(Collectors.toCollection(
                        TreeSet::new
                ))
                .stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private List<Travel> sortTravels(
            List<Travel> travels,
            String sort
    ) {
        String selectedSort = clean(sort);

        if ("price-low".equals(selectedSort)) {
            return travels
                    .stream()
                    .sorted(Comparator.comparingInt(Travel::getPrice))
                    .toList();
        }

        if ("price-high".equals(selectedSort)) {
            return travels
                    .stream()
                    .sorted(Comparator.comparingInt(Travel::getPrice).reversed())
                    .toList();
        }

        if ("hotel".equals(selectedSort)) {
            return travels
                    .stream()
                    .sorted(Comparator.comparing(Travel::getHotelName))
                    .toList();
        }

        return travels;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
