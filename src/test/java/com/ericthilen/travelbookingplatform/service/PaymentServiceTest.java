package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.dto.PaymentRequest;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.Payment;
import com.ericthilen.travelbookingplatform.model.PaymentMethod;
import com.ericthilen.travelbookingplatform.model.PaymentStatus;
import com.ericthilen.travelbookingplatform.repository.BookingRepository;
import com.ericthilen.travelbookingplatform.repository.PaymentRepository;
import com.ericthilen.travelbookingplatform.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                bookingRepository
        );
    }

    @Test
    void registerPaymentShouldSavePaymentAndUpdateBooking() {
        Booking booking = createBooking();

        PaymentRequest request =
                createRequest(
                        2_500,
                        " swish-001 "
                );

        when(
                bookingRepository.findById(1L)
        ).thenReturn(
                Optional.of(booking)
        );

        when(
                paymentRepository.existsByReferenceIgnoreCase(
                        "SWISH-001"
                )
        ).thenReturn(false);

        when(
                paymentRepository.save(
                        any(Payment.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        Payment payment =
                paymentService.registerPayment(
                        1L,
                        request
                );

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

        assertEquals(
                "SWISH-001",
                payment.getReference()
        );

        assertEquals(
                PaymentMethod.SWISH,
                payment.getPaymentMethod()
        );

        assertSame(
                booking,
                payment.getBooking()
        );

        verify(
                bookingRepository
        ).save(booking);

        verify(
                paymentRepository
        ).save(payment);
    }

    @Test
    void registerPaymentShouldMarkBookingAsPaid() {
        Booking booking = createBooking();

        PaymentRequest request =
                createRequest(
                        10_000,
                        "FINAL-001"
                );

        when(
                bookingRepository.findById(1L)
        ).thenReturn(
                Optional.of(booking)
        );

        when(
                paymentRepository.existsByReferenceIgnoreCase(
                        "FINAL-001"
                )
        ).thenReturn(false);

        when(
                paymentRepository.save(
                        any(Payment.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        paymentService.registerPayment(
                1L,
                request
        );

        assertEquals(
                PaymentStatus.PAID,
                booking.getPaymentStatus()
        );

        assertEquals(
                0,
                booking.getRemainingAmount()
        );
    }

    @Test
    void registerPaymentShouldRejectUnknownBooking() {
        when(
                bookingRepository.findById(99L)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.registerPayment(
                        99L,
                        createRequest(
                                1_000,
                                "REF-001"
                        )
                )
        );
    }

    @Test
    void registerPaymentShouldRejectDuplicateReference() {
        Booking booking = createBooking();

        when(
                bookingRepository.findById(1L)
        ).thenReturn(
                Optional.of(booking)
        );

        when(
                paymentRepository.existsByReferenceIgnoreCase(
                        "DUPLICATE"
                )
        ).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.registerPayment(
                        1L,
                        createRequest(
                                1_000,
                                "duplicate"
                        )
                )
        );

        verify(
                paymentRepository,
                never()
        ).save(
                any(Payment.class)
        );
    }

    @Test
    void registerPaymentShouldRejectAmountAboveRemainingBalance() {
        Booking booking = createBooking();

        booking.registerPayment(8_000);

        when(
                bookingRepository.findById(1L)
        ).thenReturn(
                Optional.of(booking)
        );

        when(
                paymentRepository.existsByReferenceIgnoreCase(
                        "TOO-HIGH"
                )
        ).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.registerPayment(
                        1L,
                        createRequest(
                                2_001,
                                "TOO-HIGH"
                        )
                )
        );
    }

    @Test
    void registerPaymentShouldRejectCancelledBooking() {
        Booking booking = createBooking();

        booking.cancel(0, 0);

        when(
                bookingRepository.findById(1L)
        ).thenReturn(
                Optional.of(booking)
        );

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.registerPayment(
                        1L,
                        createRequest(
                                1_000,
                                "REF-001"
                        )
                )
        );
    }

    @Test
    void registerPaymentShouldRejectAlreadyPaidBooking() {
        Booking booking = createBooking();

        booking.registerPayment(10_000);

        when(
                bookingRepository.findById(1L)
        ).thenReturn(
                Optional.of(booking)
        );

        when(
                paymentRepository.existsByReferenceIgnoreCase(
                        "REF-001"
                )
        ).thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> paymentService.registerPayment(
                        1L,
                        createRequest(
                                1,
                                "REF-001"
                        )
                )
        );
    }

    @Test
    void getBookingShouldReturnBookingFromRepository() {
        Booking booking = createBooking();

        when(
                bookingRepository.findById(1L)
        ).thenReturn(
                Optional.of(booking)
        );

        assertEquals(
                Optional.of(booking),
                paymentService.getBooking(1L)
        );
    }

    @Test
    void getBookingShouldReturnEmptyForNullId() {
        assertEquals(
                Optional.empty(),
                paymentService.getBooking(null)
        );
    }

    @Test
    void getPaymentsForBookingShouldReturnRepositoryResult() {
        Payment payment = new Payment(
                createBooking(),
                1_000,
                LocalDate.now(),
                PaymentMethod.CARD,
                "CARD-001",
                LocalDateTime.now()
        );

        when(
                paymentRepository
                        .findAllByBookingIdOrderByPaymentDateDescRegisteredAtDesc(
                                1L
                        )
        ).thenReturn(
                List.of(payment)
        );

        List<Payment> result =
                paymentService.getPaymentsForBooking(1L);

        assertEquals(
                List.of(payment),
                result
        );
    }

    @Test
    void getPaymentsForBookingShouldReturnEmptyListForNullId() {
        assertEquals(
                List.of(),
                paymentService.getPaymentsForBooking(null)
        );
    }

    @Test
    void processCustomerPaymentShouldSavePaymentWithGeneratedReference() {
        Booking booking = createBooking();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = paymentService.processCustomerPayment(1L, 2000, PaymentMethod.CARD);

        assertEquals(2000, booking.getPaidAmount());
        assertEquals(PaymentMethod.CARD, payment.getPaymentMethod());
        org.junit.jupiter.api.Assertions.assertTrue(payment.getReference().startsWith("CARD-"));
        verify(bookingRepository).save(booking);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void processCustomerPaymentShouldRejectTooHighAmount() {
        Booking booking = createBooking(); // 10 000 kr
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.processCustomerPayment(1L, 10001, PaymentMethod.SWISH)
        );
    }

    private Booking createBooking() {
        return TestDataFactory.createBooking(
                LocalDate.now().plusDays(90),
                10_000,
                1_500,
                LocalDate.now().plusDays(7)
        );
    }

    private PaymentRequest createRequest(
            int amount,
            String reference
    ) {
        PaymentRequest request =
                new PaymentRequest();

        request.setAmount(amount);
        request.setPaymentDate(
                LocalDate.now()
        );
        request.setPaymentMethod(
                PaymentMethod.SWISH
        );
        request.setReference(reference);

        return request;
    }
}