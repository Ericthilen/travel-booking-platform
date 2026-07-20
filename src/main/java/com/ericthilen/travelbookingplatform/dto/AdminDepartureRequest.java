package com.ericthilen.travelbookingplatform.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class AdminDepartureRequest {

    private Long travelId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate returnDate;

    private String departureAirport = "";
    private String arrivalAirport = "";
    private String outboundFlightNumber = "";

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime outboundDepartureTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime outboundArrivalTime;

    private String returnFlightNumber = "";

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime returnDepartureTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime returnArrivalTime;

    private int pricePerPerson;
    private int availableSeats;

    public Long getTravelId() {
        return travelId;
    }

    public void setTravelId(Long travelId) {
        this.travelId = travelId;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getDepartureAirport() {
        return departureAirport;
    }

    public void setDepartureAirport(String departureAirport) {
        this.departureAirport = departureAirport;
    }

    public String getArrivalAirport() {
        return arrivalAirport;
    }

    public void setArrivalAirport(String arrivalAirport) {
        this.arrivalAirport = arrivalAirport;
    }

    public String getOutboundFlightNumber() {
        return outboundFlightNumber;
    }

    public void setOutboundFlightNumber(String outboundFlightNumber) {
        this.outboundFlightNumber = outboundFlightNumber;
    }

    public LocalTime getOutboundDepartureTime() {
        return outboundDepartureTime;
    }

    public void setOutboundDepartureTime(LocalTime outboundDepartureTime) {
        this.outboundDepartureTime = outboundDepartureTime;
    }

    public LocalTime getOutboundArrivalTime() {
        return outboundArrivalTime;
    }

    public void setOutboundArrivalTime(LocalTime outboundArrivalTime) {
        this.outboundArrivalTime = outboundArrivalTime;
    }

    public String getReturnFlightNumber() {
        return returnFlightNumber;
    }

    public void setReturnFlightNumber(String returnFlightNumber) {
        this.returnFlightNumber = returnFlightNumber;
    }

    public LocalTime getReturnDepartureTime() {
        return returnDepartureTime;
    }

    public void setReturnDepartureTime(LocalTime returnDepartureTime) {
        this.returnDepartureTime = returnDepartureTime;
    }

    public LocalTime getReturnArrivalTime() {
        return returnArrivalTime;
    }

    public void setReturnArrivalTime(LocalTime returnArrivalTime) {
        this.returnArrivalTime = returnArrivalTime;
    }

    public int getPricePerPerson() {
        return pricePerPerson;
    }

    public void setPricePerPerson(int pricePerPerson) {
        this.pricePerPerson = pricePerPerson;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }
}
