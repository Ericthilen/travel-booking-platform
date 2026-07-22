package com.ericthilen.travelbookingplatform.dto;

import java.time.LocalDate;

public class TravelCalendarDay {

    private final LocalDate date;
    private final int dayOfMonth;
    private final boolean available;
    private final boolean selected;

    public TravelCalendarDay(
            LocalDate date,
            boolean available,
            boolean selected
    ) {
        this.date = date;
        this.dayOfMonth = date.getDayOfMonth();
        this.available = available;
        this.selected = selected;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isSelected() {
        return selected;
    }
}
