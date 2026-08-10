package com.ericthilen.travelbookingplatform.model;

public enum CustomerChatStatus {

    OPEN("Pågår"),
    WAITING_FOR_AGENT("Väntar på agent"),
    ESCALATED("Eskalerad"),
    CLOSED("Avslutad");

    private final String displayName;

    CustomerChatStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
