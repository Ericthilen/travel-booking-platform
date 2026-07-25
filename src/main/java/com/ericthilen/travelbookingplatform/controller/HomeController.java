package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.service.DepartureService;
import com.ericthilen.travelbookingplatform.service.TravelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
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
        List<HomeDepartureCard> homeDepartures =
                homeDepartures(allTravels);
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
        model.addAttribute(
                "homeDepartures",
                homeDepartures
        );
        model.addAttribute(
                "homeCountries",
                homeCountries(allTravels)
        );
        model.addAttribute(
                "homeCountryGroups",
                homeCountryGroups(allTravels)
        );
        model.addAttribute(
                "homeMapTravels",
                homeMapTravels(allTravels)
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

    private List<HomeDepartureCard> homeDepartures(List<Travel> travels) {
        List<HomeDepartureCard> cards = new ArrayList<>();

        for (Travel travel : travels) {
            for (Departure departure
                    : departureService.getDeparturesForTravel(travel.getId())) {
                cards.add(new HomeDepartureCard(
                        travel.getId(),
                        travel.getDestination(),
                        travel.getCountry(),
                        travel.getHotelName(),
                        travel.getNights(),
                        departure.getDepartureAirport(),
                        departure.getDepartureDate(),
                        departure.getDepartureDate()
                                .getMonth()
                                .getDisplayName(
                                        TextStyle.FULL,
                                        Locale.forLanguageTag("sv-SE")
                                ),
                        seasonFor(departure.getDepartureDate()),
                        departure.getPricePerPerson(),
                        departure.getAvailableSeats(),
                        travel.getImageUrl()
                ));
            }
        }

        return cards
                .stream()
                .sorted(Comparator.comparing(HomeDepartureCard::departureDate))
                .toList();
    }

    private List<String> homeCountries(List<Travel> travels) {
        return travels
                .stream()
                .map(Travel::getCountry)
                .filter(country -> country != null && !country.isBlank())
                .collect(Collectors.toCollection(TreeSet::new))
                .stream()
                .toList();
    }

    private List<HomeCountryGroup> homeCountryGroups(List<Travel> travels) {
        return homeCountries(travels)
                .stream()
                .map(country -> new HomeCountryGroup(
                        country,
                        travels
                                .stream()
                                .filter(travel -> country.equals(travel.getCountry()))
                                .sorted(Comparator.comparing(Travel::getDestination))
                                .toList()
                ))
                .toList();
    }

    private List<HomeMapTravel> homeMapTravels(List<Travel> travels) {
        return travels
                .stream()
                .map(travel -> {
                    Coordinates coordinates =
                            coordinatesFor(travel.getDestination());

                    return new HomeMapTravel(
                            travel.getId(),
                            travel.getDestination(),
                            travel.getCountry(),
                            travel.getHotelName(),
                            travel.getPrice(),
                            travel.getImageUrl(),
                            String.join(
                                    " ",
                                    seasonsFor(travel)
                            ),
                            coordinates.x(),
                            coordinates.y()
                    );
                })
                .toList();
    }

    private Set<String> seasonsFor(Travel travel) {
        return departureService
                .getDeparturesForTravel(travel.getId())
                .stream()
                .map(departure -> seasonFor(departure.getDepartureDate()))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private String seasonFor(LocalDate date) {
        Month month = date.getMonth();

        return switch (month) {
            case MARCH, APRIL, MAY -> "var";
            case JUNE, JULY, AUGUST -> "sommar";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "host";
            case DECEMBER, JANUARY, FEBRUARY -> "vinter";
        };
    }

    private Coordinates coordinatesFor(String destination) {
        return switch (destination) {
            case "Mallorca" -> mapCoordinates(39.6953, 3.0176);
            case "Kreta" -> mapCoordinates(35.2401, 24.8093);
            case "Ayia Napa" -> mapCoordinates(34.9921, 34.0140);
            case "Gran Canaria" -> mapCoordinates(27.9202, -15.5474);
            case "Sicilien" -> mapCoordinates(37.5999, 14.0154);
            case "Antalya" -> mapCoordinates(36.8969, 30.7133);
            default -> mapCoordinates(41.0, 14.0);
        };
    }

    private Coordinates mapCoordinates(
            double latitude,
            double longitude
    ) {
        double x = (longitude + 180.0) / 360.0 * 100.0;
        double y = (90.0 - latitude) / 180.0 * 100.0;

        return new Coordinates(
                x,
                y
        );
    }

    public record HomeDepartureCard(
            Long travelId,
            String destination,
            String country,
            String hotelName,
            int nights,
            String departureAirport,
            LocalDate departureDate,
            String departureMonth,
            String season,
            int price,
            int availableSeats,
            String imageUrl
    ) {
    }

    public record HomeMapTravel(
            Long travelId,
            String destination,
            String country,
            String hotelName,
            int price,
            String imageUrl,
            String seasons,
            double x,
            double y
    ) {
    }

    public record HomeCountryGroup(
            String country,
            List<Travel> travels
    ) {
    }

    private record Coordinates(
            double x,
            double y
    ) {
    }
}
