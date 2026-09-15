package com.ericthilen.travelbookingplatform.dto;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.Departure;

import java.time.LocalDate;
import java.util.List;

public class AdminDashboard {

    private final long numberOfBookings;
    private final long newBookingsToday;
    private final long unpaidBookings;
    private final long cancellations;
    private final long failedEmails;
    private final long totalSales;
    private final long paidAmount;
    private final List<Departure> upcomingDepartures;
    private final List<Booking> latestBookings;
    private final List<Booking> searchResults;
    private final String searchQuery;
    private final List<Booking> bookingRegister;
    private final String registerTravelName;
    private final LocalDate registerDepartureDate;
    private final int registerPage;
    private final int registerTotalPages;
    private final int registerTotalBookings;
    private final int registerStartEntry;
    private final int registerEndEntry;

    public AdminDashboard(
            long numberOfBookings,
            long newBookingsToday,
            long unpaidBookings,
            long cancellations,
            long failedEmails,
            long totalSales,
            long paidAmount,
            List<Departure> upcomingDepartures,
            List<Booking> latestBookings,
            List<Booking> searchResults,
            String searchQuery,
            List<Booking> bookingRegister,
            String registerTravelName,
            LocalDate registerDepartureDate,
            int registerPage,
            int registerTotalPages,
            int registerTotalBookings,
            int registerStartEntry,
            int registerEndEntry
    ) {
        this.numberOfBookings = numberOfBookings;
        this.newBookingsToday = newBookingsToday;
        this.unpaidBookings = unpaidBookings;
        this.cancellations = cancellations;
        this.failedEmails = failedEmails;
        this.totalSales = totalSales;
        this.paidAmount = paidAmount;
        this.upcomingDepartures = upcomingDepartures;
        this.latestBookings = latestBookings;
        this.searchResults = searchResults;
        this.searchQuery = searchQuery;
        this.bookingRegister = bookingRegister;
        this.registerTravelName = registerTravelName;
        this.registerDepartureDate = registerDepartureDate;
        this.registerPage = registerPage;
        this.registerTotalPages = registerTotalPages;
        this.registerTotalBookings = registerTotalBookings;
        this.registerStartEntry = registerStartEntry;
        this.registerEndEntry = registerEndEntry;
    }

    public long getNumberOfBookings() {
        return numberOfBookings;
    }

    public long getNewBookingsToday() {
        return newBookingsToday;
    }

    public long getUnpaidBookings() {
        return unpaidBookings;
    }

    public long getCancellations() {
        return cancellations;
    }

    public long getFailedEmails() {
        return failedEmails;
    }

    public long getTotalSales() {
        return totalSales;
    }

    public long getPaidAmount() {
        return paidAmount;
    }

    public List<Departure> getUpcomingDepartures() {
        return upcomingDepartures;
    }

    public List<Booking> getLatestBookings() {
        return latestBookings;
    }

    public List<Booking> getSearchResults() {
        return searchResults;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public List<Booking> getBookingRegister() {
        return bookingRegister;
    }

    public String getRegisterTravelName() {
        return registerTravelName;
    }

    public LocalDate getRegisterDepartureDate() {
        return registerDepartureDate;
    }

    public int getRegisterPage() {
        return registerPage;
    }

    public int getRegisterTotalPages() {
        return registerTotalPages;
    }

    public int getRegisterTotalBookings() {
        return registerTotalBookings;
    }

    public int getRegisterStartEntry() {
        if (registerTotalBookings == 0) {
            return 0;
        }

        return registerStartEntry;
    }

    public int getRegisterEndEntry() {
        return registerEndEntry;
    }

    public boolean hasPreviousRegisterPage() {
        return registerPage > 1;
    }

    public boolean hasNextRegisterPage() {
        return registerPage < registerTotalPages;
    }

    public boolean hasSearchQuery() {
        return searchQuery != null && !searchQuery.isBlank();
    }

    public boolean hasRegisterFilter() {
        return hasSearchQuery()
                || (registerTravelName != null && !registerTravelName.isBlank())
                || registerDepartureDate != null;
    }
}
