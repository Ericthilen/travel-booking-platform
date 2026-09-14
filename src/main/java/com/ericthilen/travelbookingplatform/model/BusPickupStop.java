package com.ericthilen.travelbookingplatform.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class BusPickupStop {

    private String city;

    private String departureTime;

    public BusPickupStop() {
    }

    public BusPickupStop(
            String city,
            String departureTime
    ) {
        this.city = city;
        this.departureTime = departureTime;
    }

    public String getCity() {
        return city;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public String getLabel() {
        return city + " kl. " + departureTime;
    }
}
