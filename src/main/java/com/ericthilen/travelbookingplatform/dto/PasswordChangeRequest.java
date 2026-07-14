package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordChangeRequest {

    @NotBlank(message = "Ange nuvarande lösenord.")
    private String currentPassword;

    @NotBlank(message = "Ange nytt lösenord.")
    @Size(min = 6, message = "Lösenordet måste vara minst 6 tecken.")
    private String newPassword;

    @NotBlank(message = "Bekräfta det nya lösenordet.")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
