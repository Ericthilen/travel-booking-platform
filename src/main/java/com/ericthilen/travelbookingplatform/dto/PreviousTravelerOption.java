package com.ericthilen.travelbookingplatform.dto;

public class PreviousTravelerOption {

    private final String personalNumber;
    private final String firstName;
    private final String lastName;

    public PreviousTravelerOption(
            String personalNumber,
            String firstName,
            String lastName
    ) {
        this.personalNumber = personalNumber;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getPersonalNumber() {
        return personalNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDisplayName() {
        return firstName + " " + lastName;
    }
}
