package com.ericthilen.travelbookingplatform.support;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.Customer;
import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.model.DiscoverySource;
import com.ericthilen.travelbookingplatform.model.PaymentPlan;
import com.ericthilen.travelbookingplatform.model.RoomType;
import com.ericthilen.travelbookingplatform.model.Travel;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Travel createTravel() {
        Travel travel = new Travel(
                "Spanien",
                "Mallorca",
                "Sun Bay Resort",
                7,
                7_495,
                "https://example.com/mallorca.jpg",
                "Ett fint hotell nära stranden.",
                "Frukost",
                "Landvetter",
                4,
                List.of(
                        "Pool",
                        "Wi-Fi"
                )
        );

        setId(travel, 1L);

        return travel;
    }

    public static Departure createDeparture(
            LocalDate departureDate
    ) {
        Departure departure = new Departure(
                createTravel(),
                departureDate,
                departureDate.plusDays(7),
                "Göteborg Landvetter",
                "Palma de Mallorca",
                "ER101",
                LocalTime.of(7, 10),
                LocalTime.of(10, 45),
                "ER102",
                LocalTime.of(18, 25),
                LocalTime.of(22, 0),
                7_495,
                30
        );

        setId(departure, 1L);

        return departure;
    }

    public static RoomType createRoomType() {
        RoomType roomType = new RoomType(
                createTravel(),
                "Standardrum",
                "Ett bekvämt standardrum.",
                2,
                0,
                10
        );

        setId(roomType, 1L);

        return roomType;
    }

    public static Customer createCustomer() {
        Customer customer = new Customer(
                "ERG-123456",
                "20030101-1234",
                "Eric",
                "Thilen",
                "0701234567",
                "eric@example.com",
                null
        );

        setId(customer, 1L);

        return customer;
    }

    public static Booking createBooking(
            LocalDate departureDate,
            int totalPrice,
            int depositAmount,
            LocalDate depositDueDate
    ) {
        PaymentPlan paymentPlan;

        if (depositAmount > 0) {
            paymentPlan = PaymentPlan.STANDARD_DEPOSIT;
        } else {
            paymentPlan = PaymentPlan.FULL_PAYMENT;
        }

        Booking booking = new Booking(
                "BK-2026-123456",
                createCustomer(),
                null,
                createDeparture(departureDate),
                createRoomType(),
                2,
                1,
                totalPrice,
                LocalDateTime.now().minusDays(1),
                DiscoverySource.WEBSITE,
                paymentPlan,
                depositAmount,
                depositDueDate,
                totalPrice,
                LocalDate.now().plusDays(30)
        );

        setId(booking, 1L);

        return booking;
    }

    public static void setId(
            Object target,
            Long id
    ) {
        try {
            Field field = target
                    .getClass()
                    .getDeclaredField("id");

            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Kunde inte sätta test-id.",
                    exception
            );
        }
    }
}