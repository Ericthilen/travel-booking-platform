package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.AssertTrue;

public class CancellationRequest {

    @AssertTrue(
            message = "Du måste bekräfta att du vill avboka bokningen."
    )
    private boolean cancellationConfirmed;

    public boolean isCancellationConfirmed() {
        return cancellationConfirmed;
    }

    public void setCancellationConfirmed(
            boolean cancellationConfirmed
    ) {
        this.cancellationConfirmed = cancellationConfirmed;
    }
}