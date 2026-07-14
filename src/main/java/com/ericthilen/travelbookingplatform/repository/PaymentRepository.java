package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    boolean existsByReferenceIgnoreCase(String reference);

    List<Payment> findAllByBookingIdOrderByPaymentDateDescRegisteredAtDesc(
            Long bookingId
    );

    List<Payment> findAllByBookingUserEmailIgnoreCaseOrderByPaymentDateDescRegisteredAtDesc(
            String email
    );
}
