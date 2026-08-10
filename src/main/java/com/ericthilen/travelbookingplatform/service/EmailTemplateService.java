package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.EmailTemplate;
import com.ericthilen.travelbookingplatform.repository.EmailTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmailTemplateService {

    private final EmailTemplateRepository templateRepository;

    public EmailTemplateService(EmailTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<EmailTemplate> getTemplates() {
        return templateRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public EmailTemplate createTemplate(String name, String content) {
        String cleanName = clean(name);
        String cleanContent = clean(content);

        if (cleanName.isBlank() || cleanContent.isBlank()) {
            throw new IllegalArgumentException("Fyll i namn och innehåll för mallen.");
        }

        return templateRepository.save(new EmailTemplate(cleanName, cleanContent));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
