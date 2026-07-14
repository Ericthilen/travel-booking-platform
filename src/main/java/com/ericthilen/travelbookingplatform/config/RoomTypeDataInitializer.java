package com.ericthilen.travelbookingplatform.config;

import com.ericthilen.travelbookingplatform.model.RoomType;
import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.repository.RoomTypeRepository;
import com.ericthilen.travelbookingplatform.repository.TravelRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RoomTypeDataInitializer {

    @Bean
    @Order(2)
    public CommandLineRunner initializeRoomTypes(
            TravelRepository travelRepository,
            RoomTypeRepository roomTypeRepository
    ) {
        return args -> {
            if (roomTypeRepository.count() > 0) {
                return;
            }

            List<Travel> travels = travelRepository.findAll();

            if (travels.isEmpty()) {
                return;
            }

            List<RoomType> roomTypes = new ArrayList<>();

            for (Travel travel : travels) {
                roomTypes.add(
                        new RoomType(
                                travel,
                                "Standardrum",
                                "Ett bekvämt rum med standardinredning, eget badrum och plats för upp till två personer.",
                                2,
                                0,
                                12
                        )
                );

                roomTypes.add(
                        new RoomType(
                                travel,
                                "Premiumrum",
                                "Ett rymligare rum med bättre läge, balkong och plats för upp till tre personer.",
                                3,
                                1500,
                                6
                        )
                );

                roomTypes.add(
                        new RoomType(
                                travel,
                                "Svit",
                                "Hotellets största rum med separat sittdel, extra komfort och plats för upp till fyra personer.",
                                4,
                                4000,
                                2
                        )
                );
            }

            roomTypeRepository.saveAll(roomTypes);
        };
    }
}