package com.ericthilen.travelbookingplatform.model;

public enum PaymentType {

    DEPOSIT("Anmälningsavgift"),
    FINAL_PAYMENT("Slutbetalning"),
    OTHER("Övrig betalning");

    private final String displayName;

    PaymentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}