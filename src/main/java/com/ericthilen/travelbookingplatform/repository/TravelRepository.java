package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.model.ManagementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TravelRepository
        extends JpaRepository<Travel, Long>, TravelSearchRepository {

    List<Travel> findAllByStatusOrderByDestinationAsc(
            ManagementStatus status
    );

    @Query("""
            select travel
            from Travel travel
            where travel.status = com.ericthilen.travelbookingplatform.model.ManagementStatus.ACTIVE
               or travel.status is null
            order by travel.destination asc
            """)
    List<Travel> findBookableTravels();
}
