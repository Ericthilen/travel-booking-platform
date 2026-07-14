package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.ArrayList;
import java.util.List;

public class BookingSelectionRequest {

    @Min(
            value = 1,
            message = "Minst en resenär måste anges."
    )
    @Max(
            value = 20,
            message = "Du kan boka för högst 20 resenärer."
    )
    private int numberOfTravelers = 2;

    @Min(
            value = 1,
            message = "Minst ett rum måste anges."
    )
    @Max(
            value = 10,
            message = "Du kan boka högst 10 rum."
    )
    private int numberOfRooms = 1;

    private List<Integer> roomOccupancies = new ArrayList<>();

    public int getNumberOfTravelers() {
        return numberOfTravelers;
    }

    public int getNumberOfRooms() {
        return numberOfRooms;
    }

    public List<Integer> getRoomOccupancies() {
        return roomOccupancies;
    }

    public void setNumberOfTravelers(int numberOfTravelers) {
        this.numberOfTravelers = numberOfTravelers;
    }

    public void setNumberOfRooms(int numberOfRooms) {
        this.numberOfRooms = numberOfRooms;
    }

    public void setRoomOccupancies(
            List<Integer> roomOccupancies
    ) {
        this.roomOccupancies = roomOccupancies;
    }
}