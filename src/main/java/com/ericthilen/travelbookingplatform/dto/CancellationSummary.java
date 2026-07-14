package com.ericthilen.travelbookingplatform.dto;

public class CancellationSummary {

    private final long daysUntilDeparture;
    private final int paidAmount;
    private final int cancellationFee;
    private final int refundAmount;
    private final int remainingAmountAfterCancellation;
    private final int nonRefundableDeposit;
    private final boolean depositLocked;

    public CancellationSummary(
            long daysUntilDeparture,
            int paidAmount,
            int cancellationFee,
            int refundAmount,
            int remainingAmountAfterCancellation,
            int nonRefundableDeposit,
            boolean depositLocked
    ) {
        this.daysUntilDeparture = daysUntilDeparture;
        this.paidAmount = paidAmount;
        this.cancellationFee = cancellationFee;
        this.refundAmount = refundAmount;
        this.remainingAmountAfterCancellation =
                remainingAmountAfterCancellation;
        this.nonRefundableDeposit = nonRefundableDeposit;
        this.depositLocked = depositLocked;
    }

    public long getDaysUntilDeparture() {
        return daysUntilDeparture;
    }

    public int getPaidAmount() {
        return paidAmount;
    }

    public int getCancellationFee() {
        return cancellationFee;
    }

    public int getRefundAmount() {
        return refundAmount;
    }

    public int getRemainingAmountAfterCancellation() {
        return remainingAmountAfterCancellation;
    }

    public int getNonRefundableDeposit() {
        return nonRefundableDeposit;
    }

    public boolean isDepositLocked() {
        return depositLocked;
    }
}