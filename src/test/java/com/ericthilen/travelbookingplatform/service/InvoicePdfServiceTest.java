package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.Invoice;
import com.ericthilen.travelbookingplatform.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoicePdfServiceTest {

    private final InvoicePdfService invoicePdfService =
            new InvoicePdfService();

    @Test
    void generateInvoicePdfShouldCreatePdfContent() {
        Booking booking =
                TestDataFactory.createBooking(
                        LocalDate.now().plusDays(90),
                        20_000,
                        3_000,
                        LocalDate.now().plusDays(7)
                );

        Invoice invoice = new Invoice(
                "INV-2026-123456",
                booking,
                LocalDateTime.now(),
                20_000,
                3_000,
                LocalDate.now().plusDays(7),
                17_000,
                LocalDate.now().plusDays(30),
                "REF-123456789012"
        );

        byte[] pdf =
                invoicePdfService
                        .generateInvoicePdf(invoice);

        assertNotNull(pdf);

        assertTrue(
                pdf.length > 100
        );

        String pdfHeader =
                new String(
                        pdf,
                        0,
                        4,
                        StandardCharsets.US_ASCII
                );

        assertTrue(
                pdfHeader.startsWith("%PDF")
        );
    }

    @Test
    void generatedPdfShouldNotBeEmptyForPaidBooking() {
        Booking booking =
                TestDataFactory.createBooking(
                        LocalDate.now().plusDays(90),
                        20_000,
                        3_000,
                        LocalDate.now().plusDays(7)
                );

        booking.registerPayment(20_000);

        Invoice invoice = new Invoice(
                "INV-2026-654321",
                booking,
                LocalDateTime.now(),
                20_000,
                3_000,
                LocalDate.now().plusDays(7),
                17_000,
                LocalDate.now().plusDays(30),
                "REF-999999999999"
        );

        byte[] pdf =
                invoicePdfService
                        .generateInvoicePdf(invoice);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
    }
}