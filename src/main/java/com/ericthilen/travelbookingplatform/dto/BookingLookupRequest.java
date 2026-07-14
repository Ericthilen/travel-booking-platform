package com.ericthilen.travelbookingplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BookingLookupRequest {

    @NotBlank(message = "Du måste ange kundnumret.")
    @Size(
            max = 30,
            message = "Kundnumret är för långt."
    )
    private String customerNumber;

    @NotBlank(message = "Du måste ange bokningsnumret.")
    @Size(
            max = 40,
            message = "Bokningsnumret är för långt."
    )
    private String bookingNumber;

    @NotBlank(message = "Du måste ange e-postadressen.")
    @Email(message = "Ange en giltig e-postadress.")
    private String email;

    public String getCustomerNumber() {
        return customerNumber;
    }

    public String getBookingNumber() {
        return bookingNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public void setBookingNumber(String bookingNumber) {
        this.bookingNumber = bookingNumber;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}