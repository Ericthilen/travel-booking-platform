package com.ericthilen.travelbookingplatform.model;

public enum PaymentMethod {

    BANK_TRANSFER("Banköverföring"),
    CARD("Kortbetalning"),
    SWISH("Swish"),
    CASH("Kontant"),
    OTHER("Övrigt");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}