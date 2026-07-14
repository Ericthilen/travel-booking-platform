package com.ericthilen.travelbookingplatform.dto;

import com.ericthilen.travelbookingplatform.model.DiscoverySource;
import com.ericthilen.travelbookingplatform.model.PaymentMethod;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public class BookingConfirmationRequest {

    @NotNull(message = "Välj hur du hittade resan.")
    private DiscoverySource discoverySource;

    @AssertTrue(message = "Du måste godkänna resevillkoren.")
    private boolean termsAccepted;

    // Payment fields
    private PaymentMethod paymentMethod;
    private String cardHolderName;
    private String cardNumber;
    private String expiryDate;
    private String cvc;
    private String phoneNumber;

    public DiscoverySource getDiscoverySource() {
        return discoverySource;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }

    public void setDiscoverySource(
            DiscoverySource discoverySource
    ) {
        this.discoverySource = discoverySource;
    }

    public void setTermsAccepted(boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCvc() {
        return cvc;
    }

    public void setCvc(String cvc) {
        this.cvc = cvc;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}