package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class BookingDetailsRequest {

    @NotBlank(message = "Du måste ange personnummer.")
    @Size(
            min = 6,
            max = 20,
            message = "Ange ett giltigt personnummer."
    )
    private String responsiblePersonalNumber;

    @NotBlank(message = "Du måste ange förnamn.")
    @Size(
            max = 100,
            message = "Förnamnet får innehålla högst 100 tecken."
    )
    private String responsibleFirstName;

    @NotBlank(message = "Du måste ange efternamn.")
    @Size(
            max = 100,
            message = "Efternamnet får innehålla högst 100 tecken."
    )
    private String responsibleLastName;

    @NotBlank(message = "Du måste ange telefonnummer.")
    @Size(
            min = 6,
            max = 30,
            message = "Ange ett giltigt telefonnummer."
    )
    private String responsiblePhone;

    @NotBlank(message = "Du måste ange e-postadress.")
    @Email(message = "Ange en giltig e-postadress.")
    private String responsibleEmail;

    @Valid
    @NotEmpty(message = "Minst en resenär måste anges.")
    private List<TravelerRequest> travelers = new ArrayList<>();

    public String getResponsiblePersonalNumber() {
        return responsiblePersonalNumber;
    }

    public String getResponsibleFirstName() {
        return responsibleFirstName;
    }

    public String getResponsibleLastName() {
        return responsibleLastName;
    }

    public String getResponsiblePhone() {
        return responsiblePhone;
    }

    public String getResponsibleEmail() {
        return responsibleEmail;
    }

    public List<TravelerRequest> getTravelers() {
        return travelers;
    }

    public void setResponsiblePersonalNumber(
            String responsiblePersonalNumber
    ) {
        this.responsiblePersonalNumber = responsiblePersonalNumber;
    }

    public void setResponsibleFirstName(
            String responsibleFirstName
    ) {
        this.responsibleFirstName = responsibleFirstName;
    }

    public void setResponsibleLastName(
            String responsibleLastName
    ) {
        this.responsibleLastName = responsibleLastName;
    }

    public void setResponsiblePhone(String responsiblePhone) {
        this.responsiblePhone = responsiblePhone;
    }

    public void setResponsibleEmail(String responsibleEmail) {
        this.responsibleEmail = responsibleEmail;
    }

    public void setTravelers(List<TravelerRequest> travelers) {
        this.travelers = travelers;
    }
}