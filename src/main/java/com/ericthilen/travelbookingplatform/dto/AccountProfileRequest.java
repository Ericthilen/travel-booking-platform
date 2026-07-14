package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AccountProfileRequest {

    @NotBlank(message = "Du måste ange namn.")
    @Size(max = 150, message = "Namnet får innehålla högst 150 tecken.")
    private String fullName;

    @Size(max = 30, message = "Telefonnumret får innehålla högst 30 tecken.")
    private String phone;

    @NotBlank(message = "Du måste ange e-post.")
    @Email(message = "Ange en giltig e-postadress.")
    private String email;

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
