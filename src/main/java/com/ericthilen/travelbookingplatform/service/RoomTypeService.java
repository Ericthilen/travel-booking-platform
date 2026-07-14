package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.RoomType;
import com.ericthilen.travelbookingplatform.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeService(
            RoomTypeRepository roomTypeRepository
    ) {
        this.roomTypeRepository = roomTypeRepository;
    }

    public List<RoomType> getRoomTypesForTravel(Long travelId) {
        return roomTypeRepository
                .findAllByTravelIdOrderByPriceSupplementPerRoomAsc(
                        travelId
                );
    }

    public Optional<RoomType> getRoomTypeById(Long id) {
        return roomTypeRepository.findById(id);
    }
}