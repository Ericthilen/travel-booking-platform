package com.ericthilen.travelbookingplatform.model;

public enum PaymentStatus {

    UNPAID("Inte betald"),
    PARTIALLY_PAID("Delvis betald"),
    PAID("Fullbetald"),
    REFUNDED("Återbetald");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}