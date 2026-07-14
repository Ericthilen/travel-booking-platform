package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository
        extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByBookingId(Long bookingId);

    boolean existsByInvoiceNumber(String invoiceNumber);

    boolean existsByPaymentReference(String paymentReference);
}