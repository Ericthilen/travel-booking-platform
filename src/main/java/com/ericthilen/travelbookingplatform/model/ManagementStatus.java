package com.ericthilen.travelbookingplatform.model;

public enum ManagementStatus {
    ACTIVE("Aktiv"),
    INACTIVE("Inaktiv"),
    SOLD_OUT("Slutsåld"),
    CANCELLED("Inställd");

    private final String displayName;

    ManagementStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
