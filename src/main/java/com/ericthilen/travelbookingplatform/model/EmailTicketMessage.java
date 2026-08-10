package com.ericthilen.travelbookingplatform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_ticket_messages")
public class EmailTicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id")
    private EmailTicket ticket;

    @Column(nullable = false, length = 40)
    private String senderType;

    @Column(nullable = false, length = 160)
    private String senderName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public EmailTicketMessage() {
    }

    public EmailTicketMessage(
            String senderType,
            String senderName,
            String message
    ) {
        this.senderType = senderType;
        this.senderName = senderName;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public void setTicket(EmailTicket ticket) {
        this.ticket = ticket;
    }

    public String getSenderType() {
        return senderType;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
