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