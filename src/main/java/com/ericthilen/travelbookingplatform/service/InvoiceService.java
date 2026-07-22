package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.Invoice;
import com.ericthilen.travelbookingplatform.repository.InvoiceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final Random random = new Random();

    public InvoiceService(
            InvoiceRepository invoiceRepository
    ) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public Invoice createInvoice(Booking booking) {
        Optional<Invoice> existingInvoice =
                invoiceRepository.findByBookingId(
                        booking.getId()
                );

        if (existingInvoice.isPresent()) {
            return existingInvoice.get();
        }

        Invoice invoice = new Invoice(
                generateInvoiceNumber(),
                booking,
                LocalDateTime.now(),
                booking.getTotalPrice(),
                booking.getDepositAmount(),
                booking.getDepositDueDate(),
                booking.getRemainingAmount(),
                booking.getFinalPaymentDueDate(),
                generatePaymentReference()
        );

        return invoiceRepository.save(invoice);
    }

    public Optional<Invoice> getInvoiceForBooking(
            Long bookingId
    ) {
        if (bookingId == null) {
            return Optional.empty();
        }

        return invoiceRepository.findByBookingId(bookingId);
    }

    @Transactional
    public Invoice regenerateInvoice(Booking booking) {
        invoiceRepository
                .findByBookingId(booking.getId())
                .ifPresent(invoiceRepository::delete);

        Invoice invoice = new Invoice(
                generateInvoiceNumber(),
                booking,
                LocalDateTime.now(),
                booking.getTotalPrice(),
                booking.getDepositAmount(),
                booking.getDepositDueDate(),
                booking.getRemainingAmount(),
                booking.getFinalPaymentDueDate(),
                generatePaymentReference()
        );

        return invoiceRepository.save(invoice);
    }

    private String generateInvoiceNumber() {
        String invoiceNumber;

        do {
            invoiceNumber = "INV-"
                    + LocalDate.now().getYear()
                    + "-"
                    + randomNumberPart();
        } while (invoiceRepository
                .existsByInvoiceNumber(invoiceNumber));

        return invoiceNumber;
    }

    private String generatePaymentReference() {
        String paymentReference;

        do {
            paymentReference = "REF-"
                    + randomNumberPart()
                    + randomNumberPart();
        } while (invoiceRepository
                .existsByPaymentReference(paymentReference));

        return paymentReference;
    }

    private String randomNumberPart() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
