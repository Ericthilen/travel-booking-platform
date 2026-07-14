package com.ericthilen.travelbookingplatform.model;

import com.ericthilen.travelbookingplatform.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingTest {

    @Test
    void registerPaymentShouldUpdatePaidAmountAndStatus() {
        Booking booking =
                bookingWithTotalPrice(10_000);

        booking.registerPayment(2_500);

        assertEquals(
                2_500,
                booking.getPaidAmount()
        );

        assertEquals(
                7_500,
                booking.getRemainingAmount()
        );

        assertEquals(
                PaymentStatus.PARTIALLY_PAID,
                booking.getPaymentStatus()
        );
    }

    @Test
    void registerPaymentShouldMarkBookingAsPaidWhenTotalIsReached() {
        Booking booking =
                bookingWithTotalPrice(10_000);

        booking.registerPayment(10_000);

        assertEquals(
                10_000,
                booking.getPaidAmount()
        );

        assertEquals(
                0,
                booking.getRemainingAmount()
        );

        assertEquals(
                PaymentStatus.PAID,
                booking.getPaymentStatus()
        );
    }

    @Test
    void registerPaymentShouldRejectZeroAmount() {
        Booking booking =
                bookingWithTotalPrice(10_000);

        assertThrows(
                IllegalArgumentException.class,
                () -> booking.registerPayment(0)
        );
    }

    @Test
    void registerPaymentShouldRejectNegativeAmount() {
        Booking booking =
                bookingWithTotalPrice(10_000);

        assertThrows(
                IllegalArgumentException.class,
                () -> booking.registerPayment(-500)
        );
    }

    @Test
    void registerPaymentShouldRejectAmountAboveRemainingBalance() {
        Booking booking =
                bookingWithTotalPrice(10_000);

        booking.registerPayment(8_000);

        assertThrows(
                IllegalArgumentException.class,
                () -> booking.registerPayment(2_001)
        );
    }

    @Test
    void registerPaymentShouldRejectCancelledBooking() {
        Booking booking =
                bookingWithTotalPrice(10_000);

        booking.cancel(0, 0);

        assertThrows(
                IllegalStateException.class,
                () -> booking.registerPayment(1_000)
        );
    }

    @Test
    void cancelShouldSetCancellationValuesAndClearRemainingBalance() {
        Booking booking =
                bookingWithTotalPrice(10_000);

        booking.registerPayment(6_000);

        booking.cancel(
                5_000,
                1_000
        );

        assertEquals(
                BookingStatus.CANCELLED,
                booking.getStatus()
        );

        assertEquals(
                5_000,
                booking.getCancellationFee()
        );

        assertEquals(
                1_000,
                booking.getRefundAmount()
        );

        assertEquals(
                0,
                booking.getRemainingAmount()
        );

        assertEquals(
                PaymentStatus.REFUNDED,
                booking.getPaymentStatus()
        );

        assertNotNull(
                booking.getCancelledAt()
        );
    }

    @Test
    void cancelWithoutRefundShouldNotMarkPaymentAsRefunded() {
        Booking booking =
                bookingWithTotalPrice(10_000);

        booking.registerPayment(4_000);

        booking.cancel(
                4_000,
                0
        );

        assertEquals(
                BookingStatus.CANCELLED,
                booking.getStatus()
        );

        assertEquals(
                0,
                booking.getRefundAmount()
        );
    }

    @Test
    void cancelShouldRejectAlreadyCancelledBooking() {
        Booking booking =
                bookingWithTotalPrice(10_000);

        booking.cancel(0, 0);

        assertThrows(
                IllegalStateException.class,
                () -> booking.cancel(0, 0)
        );
    }

    @Test
    void markBookingEmailStatusShouldUseNotSentForNull() {
        Booking booking =
                bookingWithTotalPrice(10_000);

        booking.markBookingEmailStatus(null);

        assertEquals(
                EmailStatus.NOT_SENT,
                booking.getBookingEmailStatus()
        );
    }

    @Test
    void markCancellationEmailStatusShouldSaveStatus() {
        Booking booking =
                bookingWithTotalPrice(10_000);

        booking.markCancellationEmailStatus(
                EmailStatus.SENT
        );

        assertEquals(
                EmailStatus.SENT,
                booking.getCancellationEmailStatus()
        );
    }

    private Booking bookingWithTotalPrice(
            int totalPrice
    ) {
        return TestDataFactory.createBooking(
                LocalDate.now().plusDays(90),
                totalPrice,
                1_500,
                LocalDate.now().plusDays(7)
        );
    }
}