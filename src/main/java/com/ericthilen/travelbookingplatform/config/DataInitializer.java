package com.ericthilen.travelbookingplatform.config;

import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.repository.DepartureRepository;
import com.ericthilen.travelbookingplatform.repository.TravelRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    @Order(1)
    public CommandLineRunner initializeData(
            TravelRepository travelRepository,
            DepartureRepository departureRepository
    ) {
        return args -> {
            if (travelRepository.count() == 0) {
                createTravels(travelRepository);
            }

            if (departureRepository.count() == 0) {
                createDepartures(
                        travelRepository,
                        departureRepository
                );
            }
        };
    }

    private void createTravels(
            TravelRepository travelRepository
    ) {
        Travel mallorca = new Travel(
                "Spanien",
                "Mallorca",
                "Sun Bay Resort",
                7,
                7495,
                "https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=1600&q=85",
                "Ett familjevänligt hotell nära stranden och Palma. Här bor du bekvämt med närhet till restauranger, shopping och vackra badvikar.",
                "Frukost",
                "Göteborg Landvetter",
                4,
                List.of(
                        "Pool",
                        "Barnpool",
                        "Gratis Wi-Fi",
                        "Restaurang",
                        "Gym",
                        "Nära stranden"
                )
        );

        Travel crete = new Travel(
                "Grekland",
                "Kreta",
                "Blue Coast Hotel",
                7,
                8295,
                "https://images.unsplash.com/photo-1504512485720-7d83a16ee930?auto=format&fit=crop&w=1600&q=85",
                "Njut av sol, bad och grekisk mat på ett modernt hotell nära havet. Hotellet passar både par och barnfamiljer.",
                "Halvpension",
                "Stockholm Arlanda",
                4,
                List.of(
                        "Pool",
                        "Havsutsikt",
                        "Restaurang",
                        "Poolbar",
                        "Gratis Wi-Fi",
                        "Luftkonditionering"
                )
        );

        Travel ayiaNapa = new Travel(
                "Cypern",
                "Ayia Napa",
                "Ocean View Resort",
                7,
                8995,
                "https://images.unsplash.com/photo-1530789253388-582c481c54b0?auto=format&fit=crop&w=1600&q=85",
                "Modernt hotell med stor pool och gångavstånd till stranden. Ett bra val för dig som vill kombinera bad, restauranger och nöjen.",
                "Frukost",
                "Göteborg Landvetter",
                4,
                List.of(
                        "Stor pool",
                        "Poolbar",
                        "Gym",
                        "Restaurang",
                        "Gratis Wi-Fi",
                        "Nära centrum"
                )
        );

        Travel granCanaria = new Travel(
                "Spanien",
                "Gran Canaria",
                "Palm Garden Hotel",
                7,
                9495,
                "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1600&q=85",
                "En avkopplande semester med sol nästan året runt. Hotellet har ett lugnt område och flera pooler.",
                "All Inclusive",
                "Stockholm Arlanda",
                4,
                List.of(
                        "All Inclusive",
                        "Flera pooler",
                        "Barnklubb",
                        "Restaurang",
                        "Underhållning",
                        "Gratis Wi-Fi"
                )
        );

        Travel sicily = new Travel(
                "Italien",
                "Sicilien",
                "Villa Mare",
                7,
                8795,
                "https://images.unsplash.com/photo-1529260830199-42c24126f198?auto=format&fit=crop&w=1600&q=85",
                "Upptäck italiensk mat, kultur och vackra badvikar. Villa Mare erbjuder en lugn och personlig semester.",
                "Frukost",
                "Köpenhamn Kastrup",
                3,
                List.of(
                        "Frukostbuffé",
                        "Terrass",
                        "Restaurang",
                        "Gratis Wi-Fi",
                        "Havsutsikt",
                        "Nära stranden"
                )
        );

        Travel antalya = new Travel(
                "Turkiet",
                "Antalya",
                "Golden Beach Resort",
                7,
                6995,
                "https://images.unsplash.com/photo-1564501049412-61c2a3083791?auto=format&fit=crop&w=1600&q=85",
                "Prisvärd charterresa med pool, strand och All Inclusive. Hotellet passar särskilt bra för barnfamiljer.",
                "All Inclusive",
                "Göteborg Landvetter",
                5,
                List.of(
                        "All Inclusive",
                        "Vattenrutschkanor",
                        "Barnklubb",
                        "Privat strand",
                        "Gym",
                        "Spa"
                )
        );

        travelRepository.saveAll(
                List.of(
                        mallorca,
                        crete,
                        ayiaNapa,
                        granCanaria,
                        sicily,
                        antalya
                )
        );
    }

    private void createDepartures(
            TravelRepository travelRepository,
            DepartureRepository departureRepository
    ) {
        List<Travel> travels = travelRepository.findAll();

        for (Travel travel : travels) {
            String arrivalAirport = getArrivalAirport(
                    travel.getDestination()
            );

            departureRepository.saveAll(
                    List.of(
                            createDeparture(
                                    travel,
                                    30,
                                    "ER101",
                                    "ER102",
                                    LocalTime.of(7, 10),
                                    LocalTime.of(10, 45),
                                    LocalTime.of(18, 25),
                                    LocalTime.of(22, 0),
                                    travel.getPrice(),
                                    28,
                                    arrivalAirport
                            ),
                            createDeparture(
                                    travel,
                                    44,
                                    "ER201",
                                    "ER202",
                                    LocalTime.of(11, 30),
                                    LocalTime.of(15, 5),
                                    LocalTime.of(20, 10),
                                    LocalTime.of(23, 45),
                                    travel.getPrice() + 600,
                                    14,
                                    arrivalAirport
                            ),
                            createDeparture(
                                    travel,
                                    58,
                                    "ER301",
                                    "ER302",
                                    LocalTime.of(16, 15),
                                    LocalTime.of(19, 50),
                                    LocalTime.of(9, 20),
                                    LocalTime.of(12, 55),
                                    travel.getPrice() + 1000,
                                    7,
                                    arrivalAirport
                            )
                    )
            );
        }
    }

    private Departure createDeparture(
            Travel travel,
            int daysFromNow,
            String outboundFlightNumber,
            String returnFlightNumber,
            LocalTime outboundDepartureTime,
            LocalTime outboundArrivalTime,
            LocalTime returnDepartureTime,
            LocalTime returnArrivalTime,
            int pricePerPerson,
            int availableSeats,
            String arrivalAirport
    ) {
        LocalDate departureDate = LocalDate.now()
                .plusDays(daysFromNow);

        LocalDate returnDate = departureDate
                .plusDays(travel.getNights());

        return new Departure(
                travel,
                departureDate,
                returnDate,
                travel.getDepartureAirport(),
                arrivalAirport,
                outboundFlightNumber,
                outboundDepartureTime,
                outboundArrivalTime,
                returnFlightNumber,
                returnDepartureTime,
                returnArrivalTime,
                pricePerPerson,
                availableSeats
        );
    }

    private String getArrivalAirport(String destination) {
        return switch (destination) {
            case "Mallorca" -> "Palma de Mallorca";
            case "Kreta" -> "Chania";
            case "Ayia Napa" -> "Larnaca";
            case "Gran Canaria" -> "Las Palmas";
            case "Sicilien" -> "Catania";
            case "Antalya" -> "Antalya";
            default -> destination;
        };
    }
}