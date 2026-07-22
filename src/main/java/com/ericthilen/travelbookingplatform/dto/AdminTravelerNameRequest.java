package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminTravelerNameRequest {

    @NotBlank(message = "Förnamn måste anges.")
    private String firstName;

    @NotBlank(message = "Efternamn måste anges.")
    private String lastName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
