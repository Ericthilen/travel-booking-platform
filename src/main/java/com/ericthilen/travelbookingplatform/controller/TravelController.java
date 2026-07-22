package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.TravelSearchFilters;
import com.ericthilen.travelbookingplatform.dto.TravelCalendarDay;
import com.ericthilen.travelbookingplatform.model.ManagementStatus;
import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.service.DepartureService;
import com.ericthilen.travelbookingplatform.service.TravelService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate earliestDepartureDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate latestDepartureDate,
            @RequestParam(required = false) Integer travelers,
            @RequestParam(required = false) Boolean onlyAvailable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate calendarMonth,
            @RequestParam(required = false) String sort,
            Model model
    ) {
        List<Travel> allTravels = travelService.getAllTravels();
        LocalDate selectedEarliestDepartureDate = earliestDepartureDate == null
                ? departureDate
                : earliestDepartureDate;
        TravelSearchFilters filters = new TravelSearchFilters();
        filters.setDestination(clean(destination));
        filters.setCountry(clean(country));
        filters.setDepartureAirport(clean(departureAirport));
        filters.setEarliestDepartureDate(selectedEarliestDepartureDate);
        filters.setLatestDepartureDate(latestDepartureDate);
        filters.setTravelers(positiveOrNull(travelers));
        filters.setNights(positiveOrNull(nights));
        filters.setMealType(clean(mealType));
        filters.setHotelStars(positiveOrNull(hotelStars));
        filters.setMaxPrice(positiveOrNull(maxPrice));
        filters.setOnlyAvailable(Boolean.TRUE.equals(onlyAvailable));
        filters.setPool(Boolean.TRUE.equals(pool));
        filters.setBeach(Boolean.TRUE.equals(beach));
        filters.setFamily(Boolean.TRUE.equals(family));
        filters.setSort(clean(sort));

        List<Travel> travels = travelService.searchTravels(filters);
        LocalDate calendarStart = calendarStart(
                calendarMonth,
                filters.getEarliestDepartureDate()
        );
        List<LocalDate> availableDepartureDates =
                departureService.getBookableDepartureDatesFrom(calendarStart);
        Set<LocalDate> availableDepartureDateSet =
                new HashSet<>(availableDepartureDates);

        model.addAttribute(
                "travels",
                travels
        );
        model.addAttribute(
                "allTravels",
                allTravels
        );
        model.addAttribute(
                "departureCounts",
                departureCounts(allTravels)
        );
        model.addAttribute(
                "calendarDays",
                calendarDays(
                        calendarStart,
                        availableDepartureDateSet,
                        filters.getEarliestDepartureDate()
                )
        );
        model.addAttribute(
                "calendarMonth",
                calendarStart
                        .getMonth()
                        .getDisplayName(
                                TextStyle.FULL,
                                Locale.forLanguageTag("sv-SE")
                        )
        );
        model.addAttribute(
                "calendarYear",
                calendarStart.getYear()
        );
        model.addAttribute(
                "calendarMonthValue",
                calendarStart
        );
        model.addAttribute(
                "calendarPreviousMonth",
                calendarStart.minusMonths(1)
        );
        model.addAttribute(
                "calendarNextMonth",
                calendarStart.plusMonths(1)
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
                departureService.getBookableDepartureAirports()
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
        model.addAttribute("destination", filters.getDestination());
        model.addAttribute("country", filters.getCountry());
        model.addAttribute("maxPrice", filters.getMaxPrice());
        model.addAttribute("hotelStars", filters.getHotelStars());
        model.addAttribute("nights", filters.getNights());
        model.addAttribute("mealType", filters.getMealType());
        model.addAttribute("pool", filters.isPool());
        model.addAttribute("beach", filters.isBeach());
        model.addAttribute("family", filters.isFamily());
        model.addAttribute("departureAirport", filters.getDepartureAirport());
        model.addAttribute("earliestDepartureDate", filters.getEarliestDepartureDate());
        model.addAttribute("latestDepartureDate", filters.getLatestDepartureDate());
        model.addAttribute("travelers", filters.getTravelers());
        model.addAttribute("onlyAvailable", filters.isOnlyAvailable());
        model.addAttribute("sort", filters.getSort());

        return "travels";
    }

    @GetMapping("/resor/{id}")
    public String showTravelDetails(
            @PathVariable Long id,
            Model model
    ) {
        Optional<Travel> travel = travelService.getTravelById(id);

        if (travel.isEmpty()
                || travel.get().getStatus() != ManagementStatus.ACTIVE) {
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

        if (travel.isEmpty()
                || travel.get().getStatus() != ManagementStatus.ACTIVE) {
            return "redirect:/resor";
        }

        model.addAttribute("travel", travel.get());
        model.addAttribute(
                "departures",
                departureService.getDeparturesForTravel(id)
        );

        return "travel-departures";
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

    private LocalDate calendarStart(
            LocalDate calendarMonth,
            LocalDate selectedDate
    ) {
        LocalDate date = calendarMonth != null
                ? calendarMonth
                : selectedDate;

        if (date == null) {
            date = LocalDate.now();
        }

        return date.withDayOfMonth(1);
    }

    private List<TravelCalendarDay> calendarDays(
            LocalDate calendarStart,
            Set<LocalDate> availableDepartureDates,
            LocalDate selectedDate
    ) {
        return calendarStart
                .datesUntil(calendarStart.plusDays(42))
                .map(date -> new TravelCalendarDay(
                        date,
                        availableDepartureDates.contains(date),
                        date.equals(selectedDate)
                ))
                .toList();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer positiveOrNull(Integer value) {
        if (value == null || value < 1) {
            return null;
        }

        return value;
    }
}
