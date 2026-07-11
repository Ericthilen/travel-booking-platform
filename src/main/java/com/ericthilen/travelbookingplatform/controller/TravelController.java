package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.model.Travel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class TravelController {

    private final List<Travel> travels = List.of(
            new Travel(
                    1L,
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
            ),
            new Travel(
                    2L,
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
            ),
            new Travel(
                    3L,
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
            ),
            new Travel(
                    4L,
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
            ),
            new Travel(
                    5L,
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
            ),
            new Travel(
                    6L,
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
            )
    );

    @GetMapping("/resor")
    public String showTravels(Model model) {
        model.addAttribute("travels", travels);

        return "travels";
    }

    @GetMapping("/resor/{id}")
    public String showTravelDetails(
            @PathVariable Long id,
            Model model
    ) {
        Travel travel = findTravelById(id);

        if (travel == null) {
            return "redirect:/resor";
        }

        model.addAttribute("travel", travel);

        return "travel-details";
    }

    private Travel findTravelById(Long id) {
        for (Travel travel : travels) {
            if (travel.getId().equals(id)) {
                return travel;
            }
        }

        return null;
    }
}