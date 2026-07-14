package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.repository.DepartureRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DepartureService {

    private final DepartureRepository departureRepository;

    public DepartureService(
            DepartureRepository departureRepository
    ) {
        this.departureRepository = departureRepository;
    }

    public List<Departure> getDeparturesForTravel(Long travelId) {
        return departureRepository
                .findAllByTravelIdOrderByDepartureDateAsc(travelId);
    }

    public Optional<Departure> getDepartureById(Long id) {
        return departureRepository.findById(id);
    }
}