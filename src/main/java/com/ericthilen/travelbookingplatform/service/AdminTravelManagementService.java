package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.dto.AdminDepartureRequest;
import com.ericthilen.travelbookingplatform.dto.AdminDepartureSummary;
import com.ericthilen.travelbookingplatform.dto.AdminRoomTypeRequest;
import com.ericthilen.travelbookingplatform.dto.AdminRoomTypeSummary;
import com.ericthilen.travelbookingplatform.dto.AdminTravelRequest;
import com.ericthilen.travelbookingplatform.model.BookingStatus;
import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.model.ManagementStatus;
import com.ericthilen.travelbookingplatform.model.RoomType;
import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.repository.BookingRepository;
import com.ericthilen.travelbookingplatform.repository.DepartureRepository;
import com.ericthilen.travelbookingplatform.repository.RoomTypeRepository;
import com.ericthilen.travelbookingplatform.repository.TravelRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class AdminTravelManagementService {

    private final TravelRepository travelRepository;
    private final DepartureRepository departureRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingRepository bookingRepository;

    public AdminTravelManagementService(
            TravelRepository travelRepository,
            DepartureRepository departureRepository,
            RoomTypeRepository roomTypeRepository,
            BookingRepository bookingRepository
    ) {
        this.travelRepository = travelRepository;
        this.departureRepository = departureRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<Travel> getTravels() {
        return travelRepository.findAll();
    }

    public List<AdminDepartureSummary> getDepartures() {
        return departureRepository
                .findAll()
                .stream()
                .map(departure -> new AdminDepartureSummary(
                        departure,
                        bookingRepository
                                .sumBookedSeatsForDeparture(
                                        departure.getId(),
                                        BookingStatus.CANCELLED
                                )
                                .intValue()
                ))
                .toList();
    }

    public List<AdminRoomTypeSummary> getRoomTypes() {
        return roomTypeRepository
                .findAll()
                .stream()
                .map(roomType -> new AdminRoomTypeSummary(
                        roomType,
                        bookingRepository
                                .sumBookedRoomsForRoomType(
                                        roomType.getId(),
                                        BookingStatus.CANCELLED
                                )
                                .intValue()
                ))
                .toList();
    }

    public Travel createTravel(AdminTravelRequest request) {
        Travel travel = new Travel(
                clean(request.getCountry()),
                clean(request.getDestination()),
                clean(request.getHotelName()),
                request.getNights(),
                request.getPrice(),
                clean(request.getImageUrl()),
                clean(request.getDescription()),
                clean(request.getMealType()),
                clean(request.getDepartureAirport()),
                request.getHotelStars(),
                facilitiesFromText(request.getFacilities())
        );

        return travelRepository.save(travel);
    }

    public void updateTravel(
            Long travelId,
            AdminTravelRequest request
    ) {
        Travel travel = getTravelOrThrow(travelId);

        travel.updateDetails(
                clean(request.getCountry()),
                clean(request.getDestination()),
                clean(request.getHotelName()),
                request.getNights(),
                request.getPrice(),
                clean(request.getImageUrl()),
                clean(request.getDescription()),
                clean(request.getMealType()),
                clean(request.getDepartureAirport()),
                request.getHotelStars(),
                facilitiesFromText(request.getFacilities())
        );
    }

    public void updateTravelStatus(
            Long travelId,
            ManagementStatus status
    ) {
        getTravelOrThrow(travelId).updateStatus(status);
    }

    public Departure createDeparture(AdminDepartureRequest request) {
        Travel travel = getTravelOrThrow(request.getTravelId());

        Departure departure = new Departure(
                travel,
                request.getDepartureDate(),
                request.getReturnDate(),
                clean(request.getDepartureAirport()),
                clean(request.getArrivalAirport()),
                clean(request.getOutboundFlightNumber()),
                request.getOutboundDepartureTime(),
                request.getOutboundArrivalTime(),
                clean(request.getReturnFlightNumber()),
                request.getReturnDepartureTime(),
                request.getReturnArrivalTime(),
                request.getPricePerPerson(),
                request.getAvailableSeats()
        );

        return departureRepository.save(departure);
    }

    public void updateDeparture(
            Long departureId,
            AdminDepartureRequest request
    ) {
        Departure departure = getDepartureOrThrow(departureId);
        Travel travel = getTravelOrThrow(request.getTravelId());

        departure.updateDetails(
                travel,
                request.getDepartureDate(),
                request.getReturnDate(),
                clean(request.getDepartureAirport()),
                clean(request.getArrivalAirport()),
                clean(request.getOutboundFlightNumber()),
                request.getOutboundDepartureTime(),
                request.getOutboundArrivalTime(),
                clean(request.getReturnFlightNumber()),
                request.getReturnDepartureTime(),
                request.getReturnArrivalTime(),
                request.getPricePerPerson(),
                request.getAvailableSeats()
        );
    }

    public void updateDepartureStatus(
            Long departureId,
            ManagementStatus status
    ) {
        getDepartureOrThrow(departureId).updateStatus(status);
    }

    public RoomType createRoomType(AdminRoomTypeRequest request) {
        Travel travel = getTravelOrThrow(request.getTravelId());

        RoomType roomType = new RoomType(
                travel,
                roomTypeName(request),
                roomTypeDescription(request),
                request.getMaxGuests(),
                request.getPriceSupplementPerRoom(),
                request.getAvailableRooms()
        );

        return roomTypeRepository.save(roomType);
    }

    public void updateRoomType(
            Long roomTypeId,
            AdminRoomTypeRequest request
    ) {
        RoomType roomType = getRoomTypeOrThrow(roomTypeId);
        Travel travel = getTravelOrThrow(request.getTravelId());

        roomType.updateDetails(
                travel,
                roomTypeName(request),
                roomTypeDescription(request),
                request.getMaxGuests(),
                request.getPriceSupplementPerRoom(),
                request.getAvailableRooms()
        );
    }

    public void updateRoomTypeStatus(
            Long roomTypeId,
            ManagementStatus status
    ) {
        getRoomTypeOrThrow(roomTypeId).updateStatus(status);
    }

    private Travel getTravelOrThrow(Long travelId) {
        return travelRepository
                .findById(travelId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Resan kunde inte hittas."
                        )
                );
    }

    private Departure getDepartureOrThrow(Long departureId) {
        return departureRepository
                .findById(departureId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Avgången kunde inte hittas."
                        )
                );
    }

    private RoomType getRoomTypeOrThrow(Long roomTypeId) {
        return roomTypeRepository
                .findById(roomTypeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Rumstypen kunde inte hittas."
                        )
                );
    }

    private List<String> facilitiesFromText(String facilities) {
        if (facilities == null || facilities.isBlank()) {
            return List.of();
        }

        return Arrays
                .stream(facilities.split("[,\\n]"))
                .map(String::trim)
                .filter(facility -> !facility.isBlank())
                .toList();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String roomTypeName(AdminRoomTypeRequest request) {
        String name = clean(request.getName());

        if (name.isBlank()) {
            return "Standardrum";
        }

        return name;
    }

    private String roomTypeDescription(AdminRoomTypeRequest request) {
        String description = clean(request.getDescription());

        if (!description.isBlank()) {
            return description;
        }

        return roomTypeName(request)
                + " med plats för upp till "
                + request.getMaxGuests()
                + " gäster.";
    }
}
