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

    private static final int REGISTER_PAGE_SIZE = 30;

    private final BookingRepository bookingRepository;
    private final DepartureRepository departureRepository;

    public AdminDashboardService(
            BookingRepository bookingRepository,
            DepartureRepository departureRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.departureRepository = departureRepository;
    }

    public AdminDashboard getDashboard(
            String query,
            String travelName,
            LocalDate departureDate,
            int page
    ) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        String cleanedQuery = cleanQuery(query);
        String cleanedTravelName = cleanQuery(travelName);

        List<Booking> searchResults = cleanedQuery.isBlank()
                ? List.of()
                : bookingRepository.searchAdminBookings(cleanedQuery);
        List<Booking> allRegisterBookings =
                cleanedQuery.isBlank()
                        && cleanedTravelName.isBlank()
                        && departureDate == null
                        ? bookingRepository.findAllByOrderByBookedAtDesc()
                        : bookingRepository.searchBookingRegister(
                                cleanedQuery,
                                cleanedTravelName,
                                departureDate
                        );
        int totalRegisterBookings = allRegisterBookings.size();
        int totalRegisterPages = Math.max(
                1,
                (int) Math.ceil(
                        totalRegisterBookings / (double) REGISTER_PAGE_SIZE
                )
        );
        int currentRegisterPage = Math.min(
                Math.max(1, page),
                totalRegisterPages
        );
        int pageStart = Math.min(
                (currentRegisterPage - 1) * REGISTER_PAGE_SIZE,
                totalRegisterBookings
        );
        int pageEnd = Math.min(
                pageStart + REGISTER_PAGE_SIZE,
                totalRegisterBookings
        );
        List<Booking> bookingRegister =
                allRegisterBookings.subList(pageStart, pageEnd);

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
                cleanedQuery,
                bookingRegister,
                cleanedTravelName,
                departureDate,
                currentRegisterPage,
                totalRegisterPages,
                totalRegisterBookings,
                pageStart + 1,
                pageEnd
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
