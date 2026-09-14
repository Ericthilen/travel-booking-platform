package com.ericthilen.travelbookingplatform.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            columnDefinition = "varchar(20) default 'FLIGHT'"
    )
    private TravelType travelType = TravelType.FLIGHT;

    @Column(columnDefinition = "TEXT")
    private String longDescription;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            columnDefinition = "varchar(30) default 'ACTIVE'"
    )
    private ManagementStatus status = ManagementStatus.ACTIVE;

    @ElementCollection
    @CollectionTable(
            name = "travel_facilities",
            joinColumns = @JoinColumn(name = "travel_id")
    )
    @OrderColumn(name = "facility_order")
    @Column(name = "facility", nullable = false)
    private List<String> facilities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "travel_included_items",
            joinColumns = @JoinColumn(name = "travel_id")
    )
    @OrderColumn(name = "item_order")
    @Column(name = "item", nullable = false)
    private List<String> includedItems = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "travel_day_program",
            joinColumns = @JoinColumn(name = "travel_id")
    )
    @OrderColumn(name = "day_order")
    @Column(name = "program_text", nullable = false, length = 1000)
    private List<String> dayProgram = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "travel_bus_pickup_stops",
            joinColumns = @JoinColumn(name = "travel_id")
    )
    @OrderColumn(name = "stop_order")
    private List<BusPickupStop> pickupStops = new ArrayList<>();

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
        this.longDescription = description;
        this.includedItems = defaultIncludedItems();
        this.status = ManagementStatus.ACTIVE;
    }

    public Travel(
            String country,
            String destination,
            String hotelName,
            int nights,
            int price,
            String imageUrl,
            String description,
            String longDescription,
            String mealType,
            String departureAirport,
            int hotelStars,
            TravelType travelType,
            List<String> facilities,
            List<String> includedItems,
            List<String> dayProgram,
            List<BusPickupStop> pickupStops
    ) {
        this(
                country,
                destination,
                hotelName,
                nights,
                price,
                imageUrl,
                description,
                mealType,
                departureAirport,
                hotelStars,
                facilities
        );
        this.travelType = travelType == null
                ? TravelType.FLIGHT
                : travelType;
        this.longDescription = longDescription;
        this.includedItems = new ArrayList<>(includedItems);
        this.dayProgram = new ArrayList<>(dayProgram);
        this.pickupStops = new ArrayList<>(pickupStops);
    }

    public void updateDetails(
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

    public void updateStatus(ManagementStatus status) {
        this.status = status == null ? ManagementStatus.ACTIVE : status;
    }

    public void updatePickupStops(List<BusPickupStop> pickupStops) {
        this.pickupStops = new ArrayList<>(pickupStops);
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

    public TravelType getTravelType() {
        return travelType == null ? TravelType.FLIGHT : travelType;
    }

    public boolean isBusTrip() {
        return getTravelType() == TravelType.BUS;
    }

    public boolean isFlightTrip() {
        return getTravelType() == TravelType.FLIGHT;
    }

    public String getTravelTypeLabel() {
        return isBusTrip() ? "Bussresa" : "Flygresa";
    }

    public String getTransportLabel() {
        return isBusTrip() ? "Bussresa tur och retur" : "Flyg tur och retur";
    }

    public String getDeparturePlaceLabel() {
        return isBusTrip() ? "Påstigningsorter" : "Avreseflygplats";
    }

    public String getLongDescription() {
        if (longDescription == null || longDescription.isBlank()) {
            return description;
        }

        return longDescription;
    }

    public List<String> getFacilities() {
        return facilities;
    }

    public String getFacilitiesText() {
        return String.join(
                ", ",
                facilities
        );
    }

    public ManagementStatus getStatus() {
        return status == null ? ManagementStatus.ACTIVE : status;
    }

    public List<String> getIncludedItems() {
        if (includedItems == null || includedItems.isEmpty()) {
            return defaultIncludedItems();
        }

        return includedItems;
    }

    public List<String> getDayProgram() {
        return dayProgram == null ? List.of() : dayProgram;
    }

    public List<BusPickupStop> getPickupStops() {
        return pickupStops == null ? List.of() : pickupStops;
    }

    public String getDeparturePlacesText() {
        if (!isBusTrip()) {
            return departureAirport;
        }

        return getPickupStops()
                .stream()
                .map(BusPickupStop::getCity)
                .filter(city -> city != null && !city.isBlank())
                .reduce(
                        "",
                        (places, city) -> places.isBlank()
                                ? city
                                : places + " | " + city
                );
    }

    public BusPickupStop findPickupStop(String city) {
        if (city == null || city.isBlank()) {
            return null;
        }

        return getPickupStops()
                .stream()
                .filter(stop -> stop.getCity().equals(city))
                .findFirst()
                .orElse(null);
    }

    private List<String> defaultIncludedItems() {
        return List.of(
                getTransportLabel(),
                nights + " hotellnätter",
                mealType,
                "Bagage"
        );
    }
}
