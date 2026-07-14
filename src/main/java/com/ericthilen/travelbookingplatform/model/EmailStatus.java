package com.ericthilen.travelbookingplatform.model;

public enum EmailStatus {

    NOT_SENT("Inte skickat"),
    SENT("Skickat"),
    FAILED("Misslyckades");

    private final String displayName;

    EmailStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}