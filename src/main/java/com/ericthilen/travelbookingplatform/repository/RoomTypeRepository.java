package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomTypeRepository
        extends JpaRepository<RoomType, Long> {

    List<RoomType> findAllByTravelIdOrderByPriceSupplementPerRoomAsc(
            Long travelId
    );
}