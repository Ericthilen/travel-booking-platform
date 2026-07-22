package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public class CancellationRequest {

    @NotBlank(message = "Välj varför bokningen ska avbokas.")
    private String cancellationReason;

    @AssertTrue(
            message = "Du måste bekräfta att du vill avboka bokningen."
    )
    private boolean cancellationConfirmed;

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public boolean isCancellationConfirmed() {
        return cancellationConfirmed;
    }

    public void setCancellationConfirmed(
            boolean cancellationConfirmed
    ) {
        this.cancellationConfirmed = cancellationConfirmed;
    }
}
