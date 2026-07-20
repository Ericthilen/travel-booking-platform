package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.RoomType;
import com.ericthilen.travelbookingplatform.model.ManagementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomTypeRepository
        extends JpaRepository<RoomType, Long> {

    List<RoomType> findAllByTravelIdOrderByPriceSupplementPerRoomAsc(
            Long travelId
    );

    List<RoomType> findAllByTravelIdAndStatusOrderByPriceSupplementPerRoomAsc(
            Long travelId,
            ManagementStatus status
    );

    @Query("""
            select roomType
            from RoomType roomType
            where roomType.travel.id = :travelId
              and (
                    roomType.status = com.ericthilen.travelbookingplatform.model.ManagementStatus.ACTIVE
                    or roomType.status is null
              )
            order by roomType.priceSupplementPerRoom asc
            """)
    List<RoomType> findBookableRoomTypesForTravel(
            @Param("travelId") Long travelId
    );
}
