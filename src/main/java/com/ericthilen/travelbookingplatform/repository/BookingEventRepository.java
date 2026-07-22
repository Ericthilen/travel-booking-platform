package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.BookingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingEventRepository
        extends JpaRepository<BookingEvent, Long> {

    List<BookingEvent> findAllByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
