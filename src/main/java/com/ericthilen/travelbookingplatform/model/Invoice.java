package com.ericthilen.travelbookingplatform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "booking_id",
            nullable = false,
            unique = true
    )
    private Booking booking;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private int totalAmount;

    @Column(nullable = false)
    private int depositAmount;

    private LocalDate depositDueDate;

    @Column(nullable = false)
    private int remainingAmount;

    @Column(nullable = false)
    private LocalDate finalPaymentDueDate;

    @Column(nullable = false, unique = true)
    private String paymentReference;

    public Invoice() {
    }

    public Invoice(
            String invoiceNumber,
            Booking booking,
            LocalDateTime createdAt,
            int totalAmount,
            int depositAmount,
            LocalDate depositDueDate,
            int remainingAmount,
            LocalDate finalPaymentDueDate,
            String paymentReference
    ) {
        this.invoiceNumber = invoiceNumber;
        this.booking = booking;
        this.createdAt = createdAt;
        this.totalAmount = totalAmount;
        this.depositAmount = depositAmount;
        this.depositDueDate = depositDueDate;
        this.remainingAmount = remainingAmount;
        this.finalPaymentDueDate = finalPaymentDueDate;
        this.paymentReference = paymentReference;
    }

    public Long getId() {
        return id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public Booking getBooking() {
        return booking;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public int getDepositAmount() {
        return depositAmount;
    }

    public LocalDate getDepositDueDate() {
        return depositDueDate;
    }

    public int getRemainingAmount() {
        return remainingAmount;
    }

    public LocalDate getFinalPaymentDueDate() {
        return finalPaymentDueDate;
    }

    public String getPaymentReference() {
        return paymentReference;
    }
}