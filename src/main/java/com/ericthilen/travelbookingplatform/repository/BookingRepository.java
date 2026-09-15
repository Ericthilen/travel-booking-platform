package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.BookingStatus;
import com.ericthilen.travelbookingplatform.model.EmailStatus;
import com.ericthilen.travelbookingplatform.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    boolean existsByBookingNumber(String bookingNumber);

    Optional<Booking> findByBookingNumberIgnoreCase(String bookingNumber);

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

    List<Booking> findTop150ByOrderByBookedAtDesc();

    List<Booking> findAllByOrderByBookedAtDesc();

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
            select coalesce(sum(booking.numberOfTravelers), 0)
            from Booking booking
            where booking.departure.id = :departureId
              and booking.status <> :cancelledStatus
            """)
    Long sumBookedSeatsForDeparture(
            @Param("departureId") Long departureId,
            @Param("cancelledStatus") BookingStatus cancelledStatus
    );

    @Query("""
            select coalesce(sum(booking.numberOfRooms), 0)
            from Booking booking
            where booking.roomType.id = :roomTypeId
              and booking.status <> :cancelledStatus
            """)
    Long sumBookedRoomsForRoomType(
            @Param("roomTypeId") Long roomTypeId,
            @Param("cancelledStatus") BookingStatus cancelledStatus
    );

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

    @Query("""
            select booking
            from Booking booking
            where (:query is null
                   or :query = ''
                   or lower(booking.bookingNumber) like lower(concat('%', :query, '%'))
                   or lower(booking.customer.customerNumber) like lower(concat('%', :query, '%'))
                   or lower(booking.customer.phone) like lower(concat('%', :query, '%'))
                   or lower(booking.customer.email) like lower(concat('%', :query, '%'))
                   or lower(booking.customer.firstName) like lower(concat('%', :query, '%'))
                   or lower(booking.customer.lastName) like lower(concat('%', :query, '%'))
                   or lower(concat(booking.customer.firstName, ' ', booking.customer.lastName)) like lower(concat('%', :query, '%'))
                   or lower(concat(booking.customer.lastName, ' ', booking.customer.firstName)) like lower(concat('%', :query, '%'))
                   or exists (
                       select traveler.id
                       from Traveler traveler
                       where traveler.booking = booking
                         and (
                             lower(traveler.firstName) like lower(concat('%', :query, '%'))
                             or lower(traveler.lastName) like lower(concat('%', :query, '%'))
                             or lower(concat(traveler.firstName, ' ', traveler.lastName)) like lower(concat('%', :query, '%'))
                         )
                   ))
              and (:travelName is null
                   or :travelName = ''
                   or lower(booking.departure.travel.country) like lower(concat('%', :travelName, '%'))
                   or lower(booking.departure.travel.destination) like lower(concat('%', :travelName, '%'))
                   or lower(booking.departure.travel.hotelName) like lower(concat('%', :travelName, '%')))
              and (:departureDate is null
                   or booking.departure.departureDate = :departureDate)
            order by booking.departure.departureDate desc, booking.bookedAt desc
            """)
    List<Booking> searchBookingRegister(
            @Param("query") String query,
            @Param("travelName") String travelName,
            @Param("departureDate") LocalDate departureDate
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
