package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.BookingStatus;
import com.ericthilen.travelbookingplatform.model.EmailStatus;
import com.ericthilen.travelbookingplatform.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    boolean existsByBookingNumber(String bookingNumber);

    long countByBookedAtBetween(
            java.time.LocalDateTime start,
            java.time.LocalDateTime end
    );

    long countByStatus(BookingStatus status);

    long countByPaymentStatusNotAndStatusNot(
            PaymentStatus paymentStatus,
            BookingStatus bookingStatus
    );

    long countByBookingEmailStatusOrCancellationEmailStatus(
            EmailStatus bookingEmailStatus,
            EmailStatus cancellationEmailStatus
    );

    List<Booking> findTop8ByOrderByBookedAtDesc();

    @Query("""
            select coalesce(sum(booking.totalPrice), 0)
            from Booking booking
            where booking.status <> :cancelledStatus
            """)
    Long sumTotalSales(
            @Param("cancelledStatus") BookingStatus cancelledStatus
    );

    @Query("""
            select coalesce(sum(booking.paidAmount), 0)
            from Booking booking
            """)
    Long sumPaidAmount();

    @Query("""
            select booking
            from Booking booking
            where lower(booking.bookingNumber) like lower(concat('%', :query, '%'))
               or lower(booking.customer.customerNumber) like lower(concat('%', :query, '%'))
               or lower(booking.customer.firstName) like lower(concat('%', :query, '%'))
               or lower(booking.customer.lastName) like lower(concat('%', :query, '%'))
               or lower(concat(booking.customer.firstName, ' ', booking.customer.lastName)) like lower(concat('%', :query, '%'))
               or lower(booking.customer.email) like lower(concat('%', :query, '%'))
            order by booking.bookedAt desc
            """)
    List<Booking> searchAdminBookings(
            @Param("query") String query
    );

    List<Booking> findAllByUserEmailIgnoreCaseOrderByBookedAtDesc(
            String email
    );

    List<Booking> findAllByUserEmailIgnoreCaseAndStatusOrderByBookedAtDesc(
            String email,
            com.ericthilen.travelbookingplatform.model.BookingStatus status
    );

    List<Booking> findAllByUserEmailIgnoreCaseAndStatusNotOrderByBookedAtDesc(
            String email,
            com.ericthilen.travelbookingplatform.model.BookingStatus status
    );

    List<Booking> findAllByCustomerUserEmailIgnoreCaseOrderByBookedAtDesc(
            String email
    );

    List<Booking> findAllByUserIsNullAndCustomerPersonalNumberAndCustomerEmailIgnoreCase(
            String personalNumber,
            String email
    );

    Optional<Booking> findByIdAndUserEmailIgnoreCase(
            Long id,
            String email
    );

    Optional<Booking>
    findByBookingNumberIgnoreCaseAndCustomerCustomerNumberIgnoreCaseAndCustomerEmailIgnoreCase(
            String bookingNumber,
            String customerNumber,
            String email
    );
}
