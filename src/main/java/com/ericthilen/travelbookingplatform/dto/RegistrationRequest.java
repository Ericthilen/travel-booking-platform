package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationRequest {

    @NotBlank(message = "Du måste ange ditt namn.")
    @Size(
            min = 2,
            max = 100,
            message = "Namnet måste vara mellan 2 och 100 tecken."
    )
    private String fullName;

    @NotBlank(message = "Du måste ange din e-postadress.")
    @Email(message = "Ange en giltig e-postadress.")
    private String email;

    @NotBlank(message = "Du måste ange ett lösenord.")
    @Size(
            min = 8,
            max = 100,
            message = "Lösenordet måste innehålla minst 8 tecken."
    )
    private String password;

    @NotBlank(message = "Du måste upprepa lösenordet.")
    private String confirmPassword;

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}