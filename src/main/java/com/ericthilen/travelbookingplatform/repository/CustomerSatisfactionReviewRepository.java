package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.CustomerSatisfactionReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerSatisfactionReviewRepository
        extends JpaRepository<CustomerSatisfactionReview, Long> {

    boolean existsByCaseNumber(String caseNumber);

    Optional<CustomerSatisfactionReview> findBySurveyToken(String surveyToken);

    List<CustomerSatisfactionReview> findAllByOrderBySentAtDesc();
}
