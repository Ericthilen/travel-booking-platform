package com.ericthilen.travelbookingplatform.dto;

import com.ericthilen.travelbookingplatform.model.RoomType;

public class AdminRoomTypeSummary {

    private final RoomType roomType;
    private final int bookedRooms;

    public AdminRoomTypeSummary(
            RoomType roomType,
            int bookedRooms
    ) {
        this.roomType = roomType;
        this.bookedRooms = bookedRooms;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public int getBookedRooms() {
        return bookedRooms;
    }

    public int getOpenRooms() {
        return roomType.getAvailableRooms();
    }

    public int getTotalRooms() {
        return bookedRooms + roomType.getAvailableRooms();
    }
}
