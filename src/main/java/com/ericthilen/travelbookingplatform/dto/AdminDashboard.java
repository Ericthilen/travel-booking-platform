package com.ericthilen.travelbookingplatform.dto;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.Departure;

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
            String searchQuery
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

    public boolean hasSearchQuery() {
        return searchQuery != null && !searchQuery.isBlank();
    }
}
