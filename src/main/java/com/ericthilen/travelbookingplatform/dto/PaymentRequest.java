package com.ericthilen.travelbookingplatform.dto;

import com.ericthilen.travelbookingplatform.model.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class PaymentRequest {

    @Min(
            value = 1,
            message = "Beloppet måste vara minst 1 krona."
    )
    private int amount;

    @NotNull(
            message = "Du måste välja ett betalningsdatum."
    )
    @PastOrPresent(
            message = "Betalningsdatumet kan inte ligga i framtiden."
    )
    private LocalDate paymentDate = LocalDate.now();

    @NotNull(
            message = "Du måste välja ett betalningssätt."
    )
    private PaymentMethod paymentMethod;

    @NotBlank(
            message = "Du måste ange en betalningsreferens."
    )
    @Size(
            max = 100,
            message = "Referensen får innehålla högst 100 tecken."
    )
    private String reference;

    public int getAmount() {
        return amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getReference() {
        return reference;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public void setPaymentMethod(
            PaymentMethod paymentMethod
    ) {
        this.paymentMethod = paymentMethod;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}