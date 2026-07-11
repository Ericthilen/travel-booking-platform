package com.ericthilen.travelbookingplatform.model;

public class Travel {

    private final Long id;
    private final String country;
    private final String destination;
    private final String hotelName;
    private final int nights;
    private final int price;
    private final String imageUrl;
    private final String description;

    public Travel(
            Long id,
            String country,
            String destination,
            String hotelName,
            int nights,
            int price,
            String imageUrl,
            String description
    ) {
        this.id = id;
        this.country = country;
        this.destination = destination;
        this.hotelName = hotelName;
        this.nights = nights;
        this.price = price;
        this.imageUrl = imageUrl;
        this.description = description;
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
}