package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CustomerChatIdentificationRequest {

    private Long requestMessageId;

    @Size(max = 80)
    private String customerNumber;

    @NotBlank
    @Size(max = 80)
    private String bookingNumber;

    @Size(max = 120)
    private String firstName;

    @Size(max = 120)
    private String lastName;

    public Long getRequestMessageId() {
        return requestMessageId;
    }

    public void setRequestMessageId(Long requestMessageId) {
        this.requestMessageId = requestMessageId;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getBookingNumber() {
        return bookingNumber;
    }

    public void setBookingNumber(String bookingNumber) {
        this.bookingNumber = bookingNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
