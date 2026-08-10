package com.ericthilen.travelbookingplatform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_chat_tickets")
public class CustomerChatTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, unique = true)
    private CustomerChatConversation conversation;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, length = 160)
    private String escalatedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public CustomerChatTicket() {
    }

    public CustomerChatTicket(
            CustomerChatConversation conversation,
            String title,
            String summary,
            String escalatedBy
    ) {
        this.conversation = conversation;
        this.title = title;
        this.summary = summary;
        this.escalatedBy = escalatedBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CustomerChatConversation getConversation() {
        return conversation;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getEscalatedBy() {
        return escalatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
