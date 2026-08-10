package com.ericthilen.travelbookingplatform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_satisfaction_reviews")
public class CustomerSatisfactionReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerSatisfactionCaseType caseType;

    @Column(nullable = false, unique = true, length = 40)
    private String caseNumber;

    @Column(nullable = false, unique = true, length = 80)
    private String surveyToken = UUID.randomUUID().toString();

    @Column(nullable = false, length = 180)
    private String customerEmail;

    @Column(length = 180)
    private String customerName;

    @Column(length = 180)
    private String agentName;

    @Column(nullable = false, length = 220)
    private String subject;

    @Column
    private Integer rating;

    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column
    private LocalDateTime ratedAt;

    public CustomerSatisfactionReview() {
    }

    public CustomerSatisfactionReview(
            CustomerSatisfactionCaseType caseType,
            String caseNumber,
            String customerEmail,
            String customerName,
            String agentName,
            String subject
    ) {
        this.caseType = caseType;
        this.caseNumber = caseNumber;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.agentName = agentName;
        this.subject = subject;
    }

    public void rate(int rating) {
        this.rating = rating;
        ratedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CustomerSatisfactionCaseType getCaseType() {
        return caseType;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public String getSurveyToken() {
        return surveyToken;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getSubject() {
        return subject;
    }

    public Integer getRating() {
        return rating;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getRatedAt() {
        return ratedAt;
    }
}
