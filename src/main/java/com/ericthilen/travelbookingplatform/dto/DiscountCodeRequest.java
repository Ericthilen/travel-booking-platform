package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.NotBlank;

public class DiscountCodeRequest {

    @NotBlank(message = "Rabattkod måste anges.")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
