package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.dto.CancellationSummary;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.BookingStatus;
import com.ericthilen.travelbookingplatform.model.EmailStatus;
import com.ericthilen.travelbookingplatform.model.PaymentStatus;
import com.ericthilen.travelbookingplatform.repository.BookingRepository;
import com.ericthilen.travelbookingplatform.repository.CustomerRepository;
import com.ericthilen.travelbookingplatform.repository.DepartureRepository;
import com.ericthilen.travelbookingplatform.repository.RoomTypeRepository;
import com.ericthilen.travelbookingplatform.repository.UserRepository;
import com.ericthilen.travelbookingplatform.repository.PaymentRepository;
import com.ericthilen.travelbookingplatform.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private DepartureRepository departureRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private BookingEmailService bookingEmailService;

    @Mock
    private PaymentRepository paymentRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository,
                customerRepository,
                departureRepository,
                roomTypeRepository,
                userRepository,
                paymentRepository,
                invoiceService,
                bookingEmailService
        );
    }

    @Test
    void cancellationAtLeast15DaysBeforeDepartureShouldKeepHalfOfTotalPrice() {
        Booking booking = createBooking(
                30,
                20_000,
                3_000,
                LocalDate.now().plusDays(7)
        );

        booking.registerPayment(15_000);

        CancellationSummary summary =
                bookingService.calculateCancellation(
                        booking
                );

        assertEquals(
                30,
                summary.getDaysUntilDeparture()
        );

        assertEquals(
                10_000,
                summary.getCancellationFee()
        );

        assertEquals(
                5_000,
                summary.getRefundAmount()
        );

        assertEquals(
                0,
                summary.getRemainingAmountAfterCancellation()
        );
    }

    @Test
    void cancellationExactly15DaysBeforeDepartureShouldUseFiftyPercentRule() {
        Booking booking = createBooking(
                15,
                20_000,
                3_000,
                LocalDate.now().plusDays(7)
        );

        booking.registerPayment(20_000);

        CancellationSummary summary =
                bookingService.calculateCancellation(
                        booking
                );

        assertEquals(
                10_000,
                summary.getCancellationFee()
        );

        assertEquals(
                10_000,
                summary.getRefundAmount()
        );
    }

    @Test
    void cancellation14DaysBeforeDepartureShouldRefundNothingPaid() {
        Booking booking = createBooking(
                14,
                20_000,
                3_000,
                LocalDate.now().plusDays(7)
        );

        booking.registerPayment(12_000);

        CancellationSummary summary =
                bookingService.calculateCancellation(
                        booking
                );

        assertEquals(
                12_000,
                summary.getCancellationFee()
        );

        assertEquals(
                0,
                summary.getRefundAmount()
        );

        assertEquals(
                0,
                summary.getRemainingAmountAfterCancellation()
        );
    }

    @Test
    void cancellationOneDayBeforeDepartureShouldRefundNothingPaid() {
        Booking booking = createBooking(
                1,
                20_000,
                3_000,
                LocalDate.now().plusDays(7)
        );

        booking.registerPayment(20_000);

        CancellationSummary summary =
                bookingService.calculateCancellation(
                        booking
                );

        assertEquals(
                20_000,
                summary.getCancellationFee()
        );

        assertEquals(
                0,
                summary.getRefundAmount()
        );
    }

    @Test
    void cancellationWithoutPaymentShouldNeverCreateDebt() {
        Booking booking = createBooking(
                10,
                20_000,
                3_000,
                LocalDate.now().plusDays(7)
        );

        CancellationSummary summary =
                bookingService.calculateCancellation(
                        booking
                );

        assertEquals(
                0,
                summary.getCancellationFee()
        );

        assertEquals(
                0,
                summary.getRefundAmount()
        );

        assertEquals(
                0,
                summary.getRemainingAmountAfterCancellation()
        );
    }

    @Test
    void cancellationShouldNeverChargeMoreThanCustomerHasPaid() {
        Booking booking = createBooking(
                30,
                20_000,
                3_000,
                LocalDate.now().plusDays(7)
        );

        booking.registerPayment(2_000);

        CancellationSummary summary =
                bookingService.calculateCancellation(
                        booking
                );

        assertEquals(
                2_000,
                summary.getCancellationFee()
        );

        assertEquals(
                0,
                summary.getRefundAmount()
        );

        assertEquals(
                0,
                summary.getRemainingAmountAfterCancellation()
        );
    }

    @Test
    void cancellationShouldMarkDepositAsLockedWhenDueDatePassedAndDepositPaid() {
        Booking booking = createBooking(
                40,
                20_000,
                3_000,
                LocalDate.now().minusDays(1)
        );

        booking.registerPayment(3_000);

        CancellationSummary summary =
                bookingService.calculateCancellation(
                        booking
                );

        assertTrue(
                summary.isDepositLocked()
        );

        assertEquals(
                3_000,
                summary.getNonRefundableDeposit()
        );

        assertEquals(
                3_000,
                summary.getCancellationFee()
        );

        assertEquals(
                0,
                summary.getRefundAmount()
        );
    }

    @Test
    void cancellationShouldNotLockDepositBeforeDueDate() {
        Booking booking = createBooking(
                40,
                20_000,
                3_000,
                LocalDate.now().plusDays(1)
        );

        booking.registerPayment(3_000);

        CancellationSummary summary =
                bookingService.calculateCancellation(
                        booking
                );

        assertFalse(
                summary.isDepositLocked()
        );

        assertEquals(
                0,
                summary.getNonRefundableDeposit()
        );
    }

    @Test
    void cancellationShouldNotLockUnpaidDeposit() {
        Booking booking = createBooking(
                40,
                20_000,
                3_000,
                LocalDate.now().minusDays(1)
        );

        booking.registerPayment(1_000);

        CancellationSummary summary =
                bookingService.calculateCancellation(
                        booking
                );

        assertFalse(
                summary.isDepositLocked()
        );

        assertEquals(
                0,
                summary.getNonRefundableDeposit()
        );
    }

    @Test
    void calculateCancellationShouldRejectCancelledBooking() {
        Booking booking = createBooking(
                30,
                20_000,
                3_000,
                LocalDate.now().plusDays(7)
        );

        booking.cancel(0, 0);

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.calculateCancellation(
                        booking
                )
        );
    }

    @Test
    void cancelBookingShouldReleaseCapacityAndSaveEmailStatus() {
        Booking booking = createBooking(
                30,
                20_000,
                3_000,
                LocalDate.now().plusDays(7)
        );

        booking.registerPayment(15_000);

        int seatsBefore =
                booking.getDeparture()
                        .getAvailableSeats();

        int roomsBefore =
                booking.getRoomType()
                        .getAvailableRooms();

        when(
                bookingRepository
                        .findByIdAndUserEmailIgnoreCase(
                                1L,
                                "eric@example.com"
                        )
        ).thenReturn(
                Optional.of(booking)
        );

        when(
                bookingRepository.saveAndFlush(booking)
        ).thenReturn(booking);

        when(
                bookingEmailService
                        .sendCancellationConfirmation(
                                booking
                        )
        ).thenReturn(
                EmailStatus.SENT
        );

        Booking result =
                bookingService.cancelBooking(
                        1L,
                        " eric@example.com "
                );

        assertEquals(
                BookingStatus.CANCELLED,
                result.getStatus()
        );

        assertEquals(
                PaymentStatus.REFUNDED,
                result.getPaymentStatus()
        );

        assertEquals(
                EmailStatus.SENT,
                result.getCancellationEmailStatus()
        );

        assertEquals(
                seatsBefore
                        + booking.getNumberOfTravelers(),
                booking.getDeparture()
                        .getAvailableSeats()
        );

        assertEquals(
                roomsBefore
                        + booking.getNumberOfRooms(),
                booking.getRoomType()
                        .getAvailableRooms()
        );

        assertEquals(
                0,
                result.getRemainingAmount()
        );

        verify(
                departureRepository
        ).save(
                booking.getDeparture()
        );

        verify(
                roomTypeRepository
        ).save(
                booking.getRoomType()
        );

        verify(
                bookingRepository,
                times(2)
        ).saveAndFlush(booking);
    }

    @Test
    void cancelBookingShouldRejectBlankEmail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(
                        1L,
                        " "
                )
        );
    }

    @Test
    void cancelBookingShouldRejectUnknownBooking() {
        when(
                bookingRepository
                        .findByIdAndUserEmailIgnoreCase(
                                99L,
                                "eric@example.com"
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(
                        99L,
                        "eric@example.com"
                )
        );
    }

    @Test
    void getBookingForUserShouldReturnEmptyForInvalidInput() {
        assertTrue(
                bookingService
                        .getBookingForUser(
                                null,
                                "test@example.com"
                        )
                        .isEmpty()
        );

        assertTrue(
                bookingService
                        .getBookingForUser(
                                1L,
                                " "
                        )
                        .isEmpty()
        );
    }

    @Test
    void getBookingForUserShouldUseRepository() {
        Booking booking = createBooking(
                30,
                20_000,
                3_000,
                LocalDate.now().plusDays(7)
        );

        when(
                bookingRepository
                        .findByIdAndUserEmailIgnoreCase(
                                1L,
                                "eric@example.com"
                        )
        ).thenReturn(
                Optional.of(booking)
        );

        assertEquals(
                Optional.of(booking),
                bookingService.getBookingForUser(
                        1L,
                        "eric@example.com"
                )
        );
    }

    @Test
    void getBookingsForUserShouldReturnRepositoryResult() {
        Booking booking = createBooking(
                30,
                20_000,
                3_000,
                LocalDate.now().plusDays(7)
        );

        when(
                bookingRepository
                        .findAllByUserEmailIgnoreCaseOrderByBookedAtDesc(
                                "eric@example.com"
                        )
        ).thenReturn(
                List.of(booking)
        );

        assertEquals(
                List.of(booking),
                bookingService.getBookingsForUser(
                        "eric@example.com"
                )
        );
    }

    private Booking createBooking(
            int daysUntilDeparture,
            int totalPrice,
            int depositAmount,
            LocalDate depositDueDate
    ) {
        return TestDataFactory.createBooking(
                LocalDate.now().plusDays(
                        daysUntilDeparture
                ),
                totalPrice,
                depositAmount,
                depositDueDate
        );
    }
}