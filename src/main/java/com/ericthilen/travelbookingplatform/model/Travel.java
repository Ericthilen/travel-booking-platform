package com.ericthilen.travelbookingplatform.model;

import java.util.List;

public class Travel {

    private final Long id;
    private final String country;
    private final String destination;
    private final String hotelName;
    private final int nights;
    private final int price;
    private final String imageUrl;
    private final String description;
    private final String mealType;
    private final String departureAirport;
    private final int hotelStars;
    private final List<String> facilities;

    public Travel(
            Long id,
            String country,
            String destination,
            String hotelName,
            int nights,
            int price,
            String imageUrl,
            String description,
            String mealType,
            String departureAirport,
            int hotelStars,
            List<String> facilities
    ) {
        this.id = id;
        this.country = country;
        this.destination = destination;
        this.hotelName = hotelName;
        this.nights = nights;
        this.price = price;
        this.imageUrl = imageUrl;
        this.description = description;
        this.mealType = mealType;
        this.departureAirport = departureAirport;
        this.hotelStars = hotelStars;
        this.facilities = facilities;
    }

    public Long getId() {
        return id;
    }

    public String getCountry() {
        return country;
    }

    public String getDestination() {
        return destination;
    }

    public String getHotelName() {
        return hotelName;
    }

    public int getNights() {
        return nights;
    }

    public int getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getMealType() {
        return mealType;
    }

    public String getDepartureAirport() {
        return departureAirport;
    }

    public int getHotelStars() {
        return hotelStars;
    }

    public List<String> getFacilities() {
        return facilities;
    }
}