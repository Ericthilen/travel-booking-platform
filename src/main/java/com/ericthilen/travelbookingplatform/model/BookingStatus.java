package com.ericthilen.travelbookingplatform.model;

public enum BookingStatus {

    CONFIRMED("Bekräftad"),
    CANCELLED("Avbokad");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}