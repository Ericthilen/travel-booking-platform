package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminNoteRequest {

    @NotBlank(message = "Anteckningen får inte vara tom.")
    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
