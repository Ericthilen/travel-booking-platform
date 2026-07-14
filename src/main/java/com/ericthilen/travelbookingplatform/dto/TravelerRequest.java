package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public class TravelerRequest implements Serializable {

    @NotBlank(message = "Du måste ange resenärens personnummer.")
    @Size(
            min = 6,
            max = 20,
            message = "Ange ett giltigt personnummer."
    )
    private String personalNumber;

    @NotBlank(message = "Du måste ange resenärens förnamn.")
    @Size(
            max = 100,
            message = "Förnamnet får innehålla högst 100 tecken."
    )
    private String firstName;

    @NotBlank(message = "Du måste ange resenärens efternamn.")
    @Size(
            max = 100,
            message = "Efternamnet får innehålla högst 100 tecken."
    )
    private String lastName;

    public String getPersonalNumber() {
        return personalNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setPersonalNumber(String personalNumber) {
        this.personalNumber = personalNumber;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}