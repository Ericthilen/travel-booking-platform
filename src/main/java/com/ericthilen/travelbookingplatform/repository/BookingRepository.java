package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    boolean existsByBookingNumber(String bookingNumber);

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
