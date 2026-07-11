package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.model.Travel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class TravelController {

    @GetMapping("/resor")
    public String showTravels(Model model) {
        List<Travel> travels = List.of(
                new Travel(
                        1L,
                        "Spanien",
                        "Mallorca",
                        "Sun Bay Resort",
                        7,
                        7495,
                        "https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=1000&q=80",
                        "Ett familjevänligt hotell nära stranden och Palma."
                ),
                new Travel(
                        2L,
                        "Grekland",
                        "Kreta",
                        "Blue Coast Hotel",
                        7,
                        8295,
                        "https://images.unsplash.com/photo-1504512485720-7d83a16ee930?auto=format&fit=crop&w=1000&q=80",
                        "Njut av sol, bad och grekisk mat vid havet."
                ),
                new Travel(
                        3L,
                        "Cypern",
                        "Ayia Napa",
                        "Ocean View Resort",
                        7,
                        8995,
                        "https://images.unsplash.com/photo-1530789253388-582c481c54b0?auto=format&fit=crop&w=1000&q=80",
                        "Modernt hotell med pool och gångavstånd till stranden."
                ),
                new Travel(
                        4L,
                        "Spanien",
                        "Gran Canaria",
                        "Palm Garden Hotel",
                        7,
                        9495,
                        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1000&q=80",
                        "En avkopplande semester med sol nästan året runt."
                ),
                new Travel(
                        5L,
                        "Italien",
                        "Sicilien",
                        "Villa Mare",
                        7,
                        8795,
                        "https://images.unsplash.com/photo-1529260830199-42c24126f198?auto=format&fit=crop&w=1000&q=80",
                        "Upptäck italiensk mat, kultur och vackra badvikar."
                ),
                new Travel(
                        6L,
                        "Turkiet",
                        "Antalya",
                        "Golden Beach Resort",
                        7,
                        6995,
                        "https://images.unsplash.com/photo-1564501049412-61c2a3083791?auto=format&fit=crop&w=1000&q=80",
                        "Prisvärd charterresa med pool och all inclusive."
                )
        );

        model.addAttribute("travels", travels);

        return "travels";
    }
}