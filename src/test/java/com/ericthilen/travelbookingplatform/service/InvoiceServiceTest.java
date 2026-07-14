package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.Invoice;
import com.ericthilen.travelbookingplatform.repository.InvoiceRepository;
import com.ericthilen.travelbookingplatform.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService =
                new InvoiceService(
                        invoiceRepository
                );
    }

    @Test
    void createInvoiceShouldReturnExistingInvoice() {
        Booking booking = createBooking();

        Invoice existingInvoice =
                new Invoice();

        when(
                invoiceRepository.findByBookingId(1L)
        ).thenReturn(
                Optional.of(existingInvoice)
        );

        Invoice result =
                invoiceService.createInvoice(
                        booking
                );

        assertSame(
                existingInvoice,
                result
        );

        verify(
                invoiceRepository,
                never()
        ).save(
                any(Invoice.class)
        );
    }

    @Test
    void createInvoiceShouldCopyBookingPaymentInformation() {
        Booking booking = createBooking();

        when(
                invoiceRepository.findByBookingId(1L)
        ).thenReturn(
                Optional.empty()
        );

        when(
                invoiceRepository.existsByInvoiceNumber(
                        anyString()
                )
        ).thenReturn(false);

        when(
                invoiceRepository.existsByPaymentReference(
                        anyString()
                )
        ).thenReturn(false);

        when(
                invoiceRepository.save(
                        any(Invoice.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        Invoice invoice =
                invoiceService.createInvoice(
                        booking
                );

        assertSame(
                booking,
                invoice.getBooking()
        );

        assertEquals(
                booking.getTotalPrice(),
                invoice.getTotalAmount()
        );

        assertEquals(
                booking.getDepositAmount(),
                invoice.getDepositAmount()
        );

        assertEquals(
                booking.getRemainingAmount(),
                invoice.getRemainingAmount()
        );

        assertEquals(
                booking.getDepositDueDate(),
                invoice.getDepositDueDate()
        );

        assertEquals(
                booking.getFinalPaymentDueDate(),
                invoice.getFinalPaymentDueDate()
        );

        assertTrue(
                invoice.getInvoiceNumber()
                        .startsWith("INV-")
        );

        assertTrue(
                invoice.getPaymentReference()
                        .startsWith("REF-")
        );
    }

    @Test
    void createInvoiceShouldSaveNewInvoice() {
        Booking booking = createBooking();

        when(
                invoiceRepository.findByBookingId(1L)
        ).thenReturn(
                Optional.empty()
        );

        when(
                invoiceRepository.existsByInvoiceNumber(
                        anyString()
                )
        ).thenReturn(false);

        when(
                invoiceRepository.existsByPaymentReference(
                        anyString()
                )
        ).thenReturn(false);

        when(
                invoiceRepository.save(
                        any(Invoice.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        Invoice invoice =
                invoiceService.createInvoice(
                        booking
                );

        verify(
                invoiceRepository
        ).save(invoice);
    }

    @Test
    void getInvoiceForBookingShouldReturnEmptyForNullId() {
        assertTrue(
                invoiceService
                        .getInvoiceForBooking(null)
                        .isEmpty()
        );
    }

    @Test
    void getInvoiceForBookingShouldUseRepository() {
        Invoice invoice =
                new Invoice();

        when(
                invoiceRepository.findByBookingId(1L)
        ).thenReturn(
                Optional.of(invoice)
        );

        assertEquals(
                Optional.of(invoice),
                invoiceService
                        .getInvoiceForBooking(1L)
        );
    }

    private Booking createBooking() {
        return TestDataFactory.createBooking(
                LocalDate.now().plusDays(90),
                20_000,
                3_000,
                LocalDate.now().plusDays(7)
        );
    }
}