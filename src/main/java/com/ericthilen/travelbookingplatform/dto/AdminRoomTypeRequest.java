package com.ericthilen.travelbookingplatform.dto;

public class AdminRoomTypeRequest {

    private Long travelId;
    private String name = "Standardrum";
    private String description = "";
    private int maxGuests = 2;
    private int priceSupplementPerRoom;
    private int availableRooms;

    public Long getTravelId() {
        return travelId;
    }

    public void setTravelId(Long travelId) {
        this.travelId = travelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(int maxGuests) {
        this.maxGuests = maxGuests;
    }

    public int getPriceSupplementPerRoom() {
        return priceSupplementPerRoom;
    }

    public void setPriceSupplementPerRoom(int priceSupplementPerRoom) {
        this.priceSupplementPerRoom = priceSupplementPerRoom;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(int availableRooms) {
        this.availableRooms = availableRooms;
    }
}
