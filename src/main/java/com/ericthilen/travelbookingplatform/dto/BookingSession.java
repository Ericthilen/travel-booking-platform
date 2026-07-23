package com.ericthilen.travelbookingplatform.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BookingSession implements Serializable {

    private Long departureId;

    private int numberOfTravelers;

    private int numberOfRooms;

    private List<Integer> roomOccupancies = new ArrayList<>();

    private Long roomTypeId;

    private String responsiblePersonalNumber;

    private String responsibleFirstName;

    private String responsibleLastName;

    private String responsiblePhone;

    private String responsibleEmail;

    private List<TravelerRequest> travelers = new ArrayList<>();

    private String discountCode;

    private String discountName;

    private int discountAmount;

    public Long getDepartureId() {
        return departureId;
    }

    public int getNumberOfTravelers() {
        return numberOfTravelers;
    }

    public int getNumberOfRooms() {
        return numberOfRooms;
    }

    public List<Integer> getRoomOccupancies() {
        return roomOccupancies;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public String getResponsiblePersonalNumber() {
        return responsiblePersonalNumber;
    }

    public String getResponsibleFirstName() {
        return responsibleFirstName;
    }

    public String getResponsibleLastName() {
        return responsibleLastName;
    }

    public String getResponsiblePhone() {
        return responsiblePhone;
    }

    public String getResponsibleEmail() {
        return responsibleEmail;
    }

    public List<TravelerRequest> getTravelers() {
        return travelers;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public String getDiscountName() {
        return discountName;
    }

    public int getDiscountAmount() {
        return discountAmount;
    }

    public void setDepartureId(Long departureId) {
        this.departureId = departureId;
    }

    public void setNumberOfTravelers(int numberOfTravelers) {
        this.numberOfTravelers = numberOfTravelers;
    }

    public void setNumberOfRooms(int numberOfRooms) {
        this.numberOfRooms = numberOfRooms;
    }

    public void setRoomOccupancies(
            List<Integer> roomOccupancies
    ) {
        this.roomOccupancies = new ArrayList<>(roomOccupancies);
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public void setResponsiblePersonalNumber(
            String responsiblePersonalNumber
    ) {
        this.responsiblePersonalNumber = responsiblePersonalNumber;
    }

    public void setResponsibleFirstName(
            String responsibleFirstName
    ) {
        this.responsibleFirstName = responsibleFirstName;
    }

    public void setResponsibleLastName(
            String responsibleLastName
    ) {
        this.responsibleLastName = responsibleLastName;
    }

    public void setResponsiblePhone(String responsiblePhone) {
        this.responsiblePhone = responsiblePhone;
    }

    public void setResponsibleEmail(String responsibleEmail) {
        this.responsibleEmail = responsibleEmail;
    }

    public void setTravelers(List<TravelerRequest> travelers) {
        this.travelers = new ArrayList<>(travelers);
    }

    public void applyDiscount(
            String discountCode,
            String discountName,
            int discountAmount
    ) {
        this.discountCode = discountCode;
        this.discountName = discountName;
        this.discountAmount = Math.max(0, discountAmount);
    }

    public void clearDiscount() {
        this.discountCode = null;
        this.discountName = null;
        this.discountAmount = 0;
    }
}
