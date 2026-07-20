package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.model.ManagementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DepartureRepository
        extends JpaRepository<Departure, Long> {

    List<Departure> findAllByTravelIdOrderByDepartureDateAsc(
            Long travelId
    );

    List<Departure> findAllByTravelIdAndStatusOrderByDepartureDateAsc(
            Long travelId,
            ManagementStatus status
    );

    @Query("""
            select departure
            from Departure departure
            where departure.travel.id = :travelId
              and (
                    departure.status = com.ericthilen.travelbookingplatform.model.ManagementStatus.ACTIVE
                    or departure.status is null
              )
            order by departure.departureDate asc
            """)
    List<Departure> findBookableDeparturesForTravel(
            @Param("travelId") Long travelId
    );

    List<Departure> findTop6ByDepartureDateGreaterThanEqualOrderByDepartureDateAsc(
            LocalDate date
    );
}
