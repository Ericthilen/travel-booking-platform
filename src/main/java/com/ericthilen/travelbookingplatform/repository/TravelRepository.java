package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.Travel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelRepository extends JpaRepository<Travel, Long> {
}