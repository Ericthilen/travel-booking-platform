package com.ericthilen.travelbookingplatform.dto;

import com.ericthilen.travelbookingplatform.model.Departure;

public class AdminDepartureSummary {

    private final Departure departure;
    private final int bookedSeats;

    public AdminDepartureSummary(
            Departure departure,
            int bookedSeats
    ) {
        this.departure = departure;
        this.bookedSeats = bookedSeats;
    }

    public Departure getDeparture() {
        return departure;
    }

    public int getBookedSeats() {
        return bookedSeats;
    }

    public int getOpenSeats() {
        return departure.getAvailableSeats();
    }

    public int getTotalSeats() {
        return bookedSeats + departure.getAvailableSeats();
    }
}
