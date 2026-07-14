package com.ericthilen.travelbookingplatform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TravelBookingPlatformApplicationTests {

    @Test
    void applicationClassShouldBeLoadable() {
        assertDoesNotThrow(
                () -> Class.forName(
                        "com.ericthilen.travelbookingplatform.TravelBookingPlatformApplication"
                )
        );
    }
}