package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.dto.AdminDashboard;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.BookingStatus;
import com.ericthilen.travelbookingplatform.model.EmailStatus;
import com.ericthilen.travelbookingplatform.model.PaymentStatus;
import com.ericthilen.travelbookingplatform.repository.BookingRepository;
import com.ericthilen.travelbookingplatform.repository.DepartureRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminDashboardService {

    private final BookingRepository bookingRepository;
    private final DepartureRepository departureRepository;

    public AdminDashboardService(
            BookingRepository bookingRepository,
            DepartureRepository departureRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.departureRepository = departureRepository;
    }

    public AdminDashboard getDashboard(String query) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        String cleanedQuery = cleanQuery(query);

        List<Booking> searchResults = cleanedQuery.isBlank()
                ? List.of()
                : bookingRepository.searchAdminBookings(cleanedQuery);

        return new AdminDashboard(
                bookingRepository.count(),
                bookingRepository.countByBookedAtBetween(
                        startOfDay,
                        endOfDay
                ),
                bookingRepository.countByPaymentStatusNotAndStatusNot(
                        PaymentStatus.PAID,
                        BookingStatus.CANCELLED
                ),
                bookingRepository.countByStatus(BookingStatus.CANCELLED),
                bookingRepository.countByBookingEmailStatusOrCancellationEmailStatus(
                        EmailStatus.FAILED,
                        EmailStatus.FAILED
                ),
                safeNumber(
                        bookingRepository.sumTotalSales(
                                BookingStatus.CANCELLED
                        )
                ),
                safeNumber(bookingRepository.sumPaidAmount()),
                departureRepository
                        .findTop6ByDepartureDateGreaterThanEqualOrderByDepartureDateAsc(
                                today
                        ),
                bookingRepository.findTop8ByOrderByBookedAtDesc(),
                searchResults,
                cleanedQuery
        );
    }

    private String cleanQuery(String query) {
        if (query == null) {
            return "";
        }

        return query.trim();
    }

    private long safeNumber(Long value) {
        if (value == null) {
            return 0;
        }

        return value;
    }
}
