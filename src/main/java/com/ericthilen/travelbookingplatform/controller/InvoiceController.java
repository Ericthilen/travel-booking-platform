package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.Invoice;
import com.ericthilen.travelbookingplatform.service.AdminBookingManagementService;
import com.ericthilen.travelbookingplatform.service.BookingService;
import com.ericthilen.travelbookingplatform.service.InvoicePdfService;
import com.ericthilen.travelbookingplatform.service.InvoiceService;
import com.ericthilen.travelbookingplatform.service.PaymentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Controller
public class InvoiceController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final AdminBookingManagementService adminBookingManagementService;

    public InvoiceController(
            BookingService bookingService,
            PaymentService paymentService,
            InvoiceService invoiceService,
            InvoicePdfService invoicePdfService,
            AdminBookingManagementService adminBookingManagementService
    ) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
        this.invoicePdfService = invoicePdfService;
        this.adminBookingManagementService =
                adminBookingManagementService;
    }

    @Transactional
    @GetMapping("/mina-bokningar/{bookingId}/faktura")
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        Optional<Booking> booking =
                bookingService.getBookingForUser(
                        bookingId,
                        authentication.getName()
                );

        if (booking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Invoice invoice = invoiceService
                .getInvoiceForBooking(bookingId)
                .orElseGet(() ->
                        invoiceService.createInvoice(
                                booking.get()
                        )
                );

        byte[] pdf =
                invoicePdfService.generateInvoicePdf(invoice);
        adminBookingManagementService.logEvent(
                booking.get(),
                "Faktura hämtad",
                "Kunden hämtade faktura "
                        + invoice.getInvoiceNumber()
                        + ".",
                authentication.getName()
        );

        String filename =
                "Faktura-"
                        + invoice.getInvoiceNumber()
                        + ".pdf";

        ContentDisposition contentDisposition =
                ContentDisposition.attachment()
                        .filename(
                                filename,
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @Transactional
    @GetMapping("/admin/bokningar/{bookingId}/faktura")
    public ResponseEntity<byte[]> downloadInvoiceForAdmin(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        Optional<Booking> booking =
                paymentService.getBooking(bookingId);

        if (booking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Invoice invoice = invoiceService
                .getInvoiceForBooking(bookingId)
                .orElseGet(() ->
                        invoiceService.createInvoice(
                                booking.get()
                        )
                );

        byte[] pdf =
                invoicePdfService.generateInvoicePdf(invoice);
        adminBookingManagementService.logEvent(
                booking.get(),
                "Faktura hämtad",
                "Admin hämtade faktura "
                        + invoice.getInvoiceNumber()
                        + ".",
                authentication == null ? "Admin" : authentication.getName()
        );

        String filename =
                "Faktura-"
                        + invoice.getInvoiceNumber()
                        + ".pdf";

        ContentDisposition contentDisposition =
                ContentDisposition.attachment()
                        .filename(
                                filename,
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
