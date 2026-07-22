package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.dto.TravelSearchFilters;
import com.ericthilen.travelbookingplatform.model.Travel;

import java.util.List;

public interface TravelSearchRepository {

    List<Travel> searchTravels(TravelSearchFilters filters);
}
