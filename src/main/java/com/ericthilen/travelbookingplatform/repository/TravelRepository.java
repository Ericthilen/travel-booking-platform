package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.model.ManagementStatus;
import com.ericthilen.travelbookingplatform.model.TravelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelRepository
        extends JpaRepository<Travel, Long>, TravelSearchRepository {

    List<Travel> findAllByStatusOrderByDestinationAsc(
            ManagementStatus status
    );

    boolean existsByDestinationAndTravelType(
            String destination,
            TravelType travelType
    );

    Optional<Travel> findByDestinationAndTravelType(
            String destination,
            TravelType travelType
    );

    @Query("""
            select travel
            from Travel travel
            where travel.status = com.ericthilen.travelbookingplatform.model.ManagementStatus.ACTIVE
               or travel.status is null
            order by travel.destination asc
            """)
    List<Travel> findBookableTravels();

    @Query("""
            select travel
            from Travel travel
            where (travel.status = com.ericthilen.travelbookingplatform.model.ManagementStatus.ACTIVE
               or travel.status is null)
              and (travel.travelType = :travelType)
            order by travel.destination asc
            """)
    List<Travel> findBookableTravelsByType(
            @Param("travelType") TravelType travelType
    );
}
