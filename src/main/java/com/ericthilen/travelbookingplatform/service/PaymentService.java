package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.dto.PaymentRequest;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.BookingStatus;
import com.ericthilen.travelbookingplatform.model.Payment;
import com.ericthilen.travelbookingplatform.model.PaymentMethod;
import com.ericthilen.travelbookingplatform.repository.BookingRepository;
import com.ericthilen.travelbookingplatform.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final Random random = new Random();

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    public Optional<Booking> getBooking(Long bookingId) {
        if (bookingId == null) {
            return Optional.empty();
        }

        return bookingRepository.findById(bookingId);
    }

    public List<Payment> getPaymentsForBooking(
            Long bookingId
    ) {
        if (bookingId == null) {
            return List.of();
        }

        return paymentRepository
                .findAllByBookingIdOrderByPaymentDateDescRegisteredAtDesc(
                        bookingId
                );
    }

    @Transactional
    public Payment processCustomerPayment(
            Long bookingId,
            int amount,
            PaymentMethod method
    ) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Bokningen kunde inte hittas."));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Det går inte att betala en avbokad bokning.");
        }

        int remainingAmount = booking.getTotalPrice() - booking.getPaidAmount();
        if (amount > remainingAmount) {
            throw new IllegalArgumentException("Beloppet får inte överstiga kvarvarande belopp (" + remainingAmount + " kr).");
        }

        String reference = generateReference(method);

        booking.registerPayment(amount);
        bookingRepository.save(booking);

        Payment payment = new Payment(
                booking,
                amount,
                LocalDate.now(),
                method,
                reference,
                LocalDateTime.now()
        );

        return paymentRepository.save(payment);
    }

    private String generateReference(PaymentMethod method) {
        String prefix = switch (method) {
            case CARD -> "CARD-";
            case SWISH -> "SWSH-";
            default -> "PAY-";
        };
        return prefix + String.format("%08d", random.nextInt(100_000_000));
    }

    @Transactional
    public Payment registerPayment(
            Long bookingId,
            PaymentRequest paymentRequest
    ) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bokningen kunde inte hittas."
                        )
                );

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Det går inte att registrera en betalning "
                            + "på en avbokad bokning."
            );
        }

        String normalizedReference =
                normalizeReference(paymentRequest.getReference());

        if (paymentRepository
                .existsByReferenceIgnoreCase(normalizedReference)) {

            throw new IllegalArgumentException(
                    "Det finns redan en betalning med denna referens."
            );
        }

        int remainingAmount =
                booking.getTotalPrice() - booking.getPaidAmount();

        if (remainingAmount <= 0) {
            throw new IllegalStateException(
                    "Bokningen är redan fullbetald."
            );
        }

        if (paymentRequest.getAmount() > remainingAmount) {
            throw new IllegalArgumentException(
                    "Betalningen får inte vara högre än det "
                            + "återstående beloppet på "
                            + formatMoney(remainingAmount) + "."
            );
        }

        booking.registerPayment(
                paymentRequest.getAmount()
        );

        Payment payment = new Payment(
                booking,
                paymentRequest.getAmount(),
                paymentRequest.getPaymentDate(),
                paymentRequest.getPaymentMethod(),
                normalizedReference,
                LocalDateTime.now()
        );

        bookingRepository.save(booking);

        return paymentRepository.save(payment);
    }

    private String normalizeReference(String reference) {
        if (reference == null) {
            return "";
        }

        return reference
                .trim()
                .toUpperCase();
    }

    private String formatMoney(int amount) {
        return String.format("%,d kr", amount)
                .replace(',', ' ');
    }
}