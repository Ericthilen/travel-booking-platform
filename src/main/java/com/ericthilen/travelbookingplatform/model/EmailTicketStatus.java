package com.ericthilen.travelbookingplatform.model;

public enum EmailTicketStatus {

    OPEN("Öppen"),
    WAITING_ON_US("Väntar på ytterligare svar från oss"),
    WAITING("Väntande"),
    ESCALATED("Eskalerad"),
    CLOSED("Avslutad");

    private final String displayName;

    EmailTicketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
