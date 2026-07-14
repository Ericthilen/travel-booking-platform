package com.ericthilen.travelbookingplatform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_types")
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private int maxGuests;

    @Column(nullable = false)
    private int priceSupplementPerRoom;

    @Column(nullable = false)
    private int availableRooms;

    public RoomType() {
    }

    public RoomType(
            Travel travel,
            String name,
            String description,
            int maxGuests,
            int priceSupplementPerRoom,
            int availableRooms
    ) {
        this.travel = travel;
        this.name = name;
        this.description = description;
        this.maxGuests = maxGuests;
        this.priceSupplementPerRoom = priceSupplementPerRoom;
        this.availableRooms = availableRooms;
    }

    public void reserveRooms(int numberOfRooms) {
        if (numberOfRooms < 1) {
            throw new IllegalArgumentException(
                    "Antalet rum måste vara minst ett."
            );
        }

        if (numberOfRooms > availableRooms) {
            throw new IllegalStateException(
                    "Det finns inte tillräckligt många rum kvar."
            );
        }

        availableRooms -= numberOfRooms;
    }

    public void releaseRooms(int numberOfRooms) {
        if (numberOfRooms < 1) {
            throw new IllegalArgumentException(
                    "Antalet rum måste vara minst ett."
            );
        }

        availableRooms += numberOfRooms;
    }

    public Long getId() {
        return id;
    }

    public Travel getTravel() {
        return travel;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public int getPriceSupplementPerRoom() {
        return priceSupplementPerRoom;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }
}