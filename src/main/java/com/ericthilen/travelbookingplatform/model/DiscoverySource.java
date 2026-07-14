package com.ericthilen.travelbookingplatform.model;

public enum DiscoverySource {

    WEBSITE("EriGo Travels hemsida"),
    GOOGLE("Google"),
    FACEBOOK("Facebook"),
    INSTAGRAM("Instagram"),
    TIKTOK("TikTok"),
    RECOMMENDATION("Rekommendation från vän eller familj"),
    PREVIOUS_CUSTOMER("Tidigare kund"),
    OTHER("Annat");

    private final String displayName;

    DiscoverySource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}