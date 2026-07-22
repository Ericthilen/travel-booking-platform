package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.dto.TravelSearchFilters;
import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.repository.TravelRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TravelService {

    private final TravelRepository travelRepository;

    public TravelService(TravelRepository travelRepository) {
        this.travelRepository = travelRepository;
    }

    public List<Travel> getAllTravels() {
        return travelRepository
                .findBookableTravels();
    }

    public List<Travel> searchTravels(TravelSearchFilters filters) {
        return travelRepository.searchTravels(filters);
    }

    public List<Travel> getAllTravelsForAdmin() {
        return travelRepository.findAll();
    }

    public Optional<Travel> getTravelById(Long id) {
        return travelRepository.findById(id);
    }
}
