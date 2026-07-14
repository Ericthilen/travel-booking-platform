package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.Departure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartureRepository
        extends JpaRepository<Departure, Long> {

    List<Departure> findAllByTravelIdOrderByDepartureDateAsc(
            Long travelId
    );
}