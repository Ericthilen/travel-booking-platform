package com.ericthilen.travelbookingplatform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;

@Entity
@Table(name = "departures")
public class Departure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @Column(nullable = false)
    private LocalDate departureDate;

    @Column(nullable = false)
    private LocalDate returnDate;

    @Column(nullable = false)
    private String departureAirport;

    @Column(nullable = false)
    private String arrivalAirport;

    @Column(nullable = false)
    private String outboundFlightNumber;

    @Column(nullable = false)
    private LocalTime outboundDepartureTime;

    @Column(nullable = false)
    private LocalTime outboundArrivalTime;

    @Column(nullable = false)
    private String returnFlightNumber;

    @Column(nullable = false)
    private LocalTime returnDepartureTime;

    @Column(nullable = false)
    private LocalTime returnArrivalTime;

    @Column(nullable = false)
    private int pricePerPerson;

    @Column(nullable = false)
    private int availableSeats;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            columnDefinition = "varchar(30) default 'ACTIVE'"
    )
    private ManagementStatus status = ManagementStatus.ACTIVE;

    public Departure() {
    }

    public Departure(
            Travel travel,
            LocalDate departureDate,
            LocalDate returnDate,
            String departureAirport,
            String arrivalAirport,
            String outboundFlightNumber,
            LocalTime outboundDepartureTime,
            LocalTime outboundArrivalTime,
            String returnFlightNumber,
            LocalTime returnDepartureTime,
            LocalTime returnArrivalTime,
            int pricePerPerson,
            int availableSeats
    ) {
        this.travel = travel;
        this.departureDate = departureDate;
        this.returnDate = returnDate;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.outboundFlightNumber = outboundFlightNumber;
        this.outboundDepartureTime = outboundDepartureTime;
        this.outboundArrivalTime = outboundArrivalTime;
        this.returnFlightNumber = returnFlightNumber;
        this.returnDepartureTime = returnDepartureTime;
        this.returnArrivalTime = returnArrivalTime;
        this.pricePerPerson = pricePerPerson;
        this.availableSeats = availableSeats;
        this.status = ManagementStatus.ACTIVE;
    }

    public void updateDetails(
            Travel travel,
            LocalDate departureDate,
            LocalDate returnDate,
            String departureAirport,
            String arrivalAirport,
            String outboundFlightNumber,
            LocalTime outboundDepartureTime,
            LocalTime outboundArrivalTime,
            String returnFlightNumber,
            LocalTime returnDepartureTime,
            LocalTime returnArrivalTime,
            int pricePerPerson,
            int availableSeats
    ) {
        this.travel = travel;
        this.departureDate = departureDate;
        this.returnDate = returnDate;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.outboundFlightNumber = outboundFlightNumber;
        this.outboundDepartureTime = outboundDepartureTime;
        this.outboundArrivalTime = outboundArrivalTime;
        this.returnFlightNumber = returnFlightNumber;
        this.returnDepartureTime = returnDepartureTime;
        this.returnArrivalTime = returnArrivalTime;
        this.pricePerPerson = pricePerPerson;
        this.availableSeats = availableSeats;
    }

    public void updateStatus(ManagementStatus status) {
        this.status = status == null ? ManagementStatus.ACTIVE : status;
    }

    public void reserveSeats(int numberOfSeats) {
        if (numberOfSeats < 1) {
            throw new IllegalArgumentException(
                    "Antalet platser måste vara minst en."
            );
        }

        if (numberOfSeats > availableSeats) {
            throw new IllegalStateException(
                    "Det finns inte tillräckligt många platser kvar."
            );
        }

        availableSeats -= numberOfSeats;
    }

    public void releaseSeats(int numberOfSeats) {
        if (numberOfSeats < 1) {
            throw new IllegalArgumentException(
                    "Antalet platser måste vara minst en."
            );
        }

        availableSeats += numberOfSeats;
    }

    public Long getId() {
        return id;
    }

    public Travel getTravel() {
        return travel;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public String getDepartureAirport() {
        return departureAirport;
    }

    public String getArrivalAirport() {
        return arrivalAirport;
    }

    public String getOutboundFlightNumber() {
        return outboundFlightNumber;
    }

    public LocalTime getOutboundDepartureTime() {
        return outboundDepartureTime;
    }

    public LocalTime getOutboundArrivalTime() {
        return outboundArrivalTime;
    }

    public String getReturnFlightNumber() {
        return returnFlightNumber;
    }

    public LocalTime getReturnDepartureTime() {
        return returnDepartureTime;
    }

    public LocalTime getReturnArrivalTime() {
        return returnArrivalTime;
    }

    public String getOutboundDurationLabel() {
        return formatDuration(
                outboundDepartureTime,
                outboundArrivalTime
        );
    }

    public String getReturnDurationLabel() {
        return formatDuration(
                returnDepartureTime,
                returnArrivalTime
        );
    }

    public String getDepartureAirportCode() {
        return airportCode(departureAirport);
    }

    public String getArrivalAirportCode() {
        return airportCode(arrivalAirport);
    }

    public int getPricePerPerson() {
        return pricePerPerson;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public ManagementStatus getStatus() {
        return status == null ? ManagementStatus.ACTIVE : status;
    }

    private String formatDuration(
            LocalTime departureTime,
            LocalTime arrivalTime
    ) {
        Duration duration = Duration.between(
                departureTime,
                arrivalTime
        );

        if (duration.isNegative()) {
            duration = duration.plusHours(24);
        }

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        return hours + "h " + minutes + "m";
    }

    private String airportCode(String airport) {
        if (airport == null) {
            return "";
        }

        return switch (airport) {
            case "Stockholm Arlanda" -> "ARN";
            case "Göteborg Landvetter" -> "GOT";
            case "Köpenhamn Kastrup" -> "CPH";
            case "Palma de Mallorca" -> "PMI";
            case "Chania" -> "CHQ";
            case "Larnaca" -> "LCA";
            case "Las Palmas" -> "LPA";
            case "Catania" -> "CTA";
            case "Antalya" -> "AYT";
            default -> "";
        };
    }
}
