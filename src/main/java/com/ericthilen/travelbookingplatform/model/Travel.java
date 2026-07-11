package com.ericthilen.travelbookingplatform.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "travels")
public class Travel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private String hotelName;

    @Column(nullable = false)
    private int nights;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String mealType;

    @Column(nullable = false)
    private String departureAirport;

    @Column(nullable = false)
    private int hotelStars;

    @ElementCollection
    @CollectionTable(
            name = "travel_facilities",
            joinColumns = @JoinColumn(name = "travel_id")
    )
    @OrderColumn(name = "facility_order")
    @Column(name = "facility", nullable = false)
    private List<String> facilities = new ArrayList<>();

    public Travel() {
    }

    public Travel(
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
        this.facilities = new ArrayList<>(facilities);
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