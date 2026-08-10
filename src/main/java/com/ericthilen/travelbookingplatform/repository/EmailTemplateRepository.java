package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    List<EmailTemplate> findAllByOrderByCreatedAtDesc();
}
