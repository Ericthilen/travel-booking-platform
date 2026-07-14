package com.ericthilen.travelbookingplatform.dto;

import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.model.RoomType;

public record BookingSummary(
        Departure departure,
        RoomType roomType,
        int numberOfTravelers,
        int numberOfRooms,
        int travelPrice,
        int roomSupplement,
        int totalPrice,
        int depositAmount,
        boolean immediatePaymentRequired
) {
}
