package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AdminBookingContactRequest {

    @NotBlank(message = "Förnamn måste anges.")
    private String firstName;

    @NotBlank(message = "Efternamn måste anges.")
    private String lastName;

    @NotBlank(message = "Telefonnummer måste anges.")
    private String phone;

    @NotBlank(message = "E-post måste anges.")
    @Email(message = "Ange en giltig e-postadress.")
    private String email;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
