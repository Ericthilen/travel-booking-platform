package com.ericthilen.travelbookingplatform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_chat_messages")
public class CustomerChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private CustomerChatConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerChatSender sender;

    @Column(nullable = false, length = 120)
    private String authorName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime editedAt;

    @Column(length = 80)
    private String detectedBookingNumber;

    private Long detectedBookingId;

    @Column(nullable = false)
    private boolean identificationRequest = false;

    @Column(nullable = false)
    private boolean identificationSubmitted = false;

    public CustomerChatMessage() {
    }

    public CustomerChatMessage(
            CustomerChatSender sender,
            String authorName,
            String message
    ) {
        this.sender = sender;
        this.authorName = authorName;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CustomerChatConversation getConversation() {
        return conversation;
    }

    public void setConversation(CustomerChatConversation conversation) {
        this.conversation = conversation;
    }

    public CustomerChatSender getSender() {
        return sender;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getMessage() {
        return message;
    }

    public void updateMessage(String message) {
        this.message = message;
        this.editedAt = LocalDateTime.now();
    }

    public void attachDetectedBooking(
            Long bookingId,
            String bookingNumber
    ) {
        this.detectedBookingId = bookingId;
        this.detectedBookingNumber = bookingNumber;
    }

    public void markAsIdentificationRequest() {
        this.identificationRequest = true;
    }

    public void markIdentificationSubmitted() {
        this.identificationSubmitted = true;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public boolean isEdited() {
        return editedAt != null;
    }

    public String getDetectedBookingNumber() {
        return detectedBookingNumber;
    }

    public Long getDetectedBookingId() {
        return detectedBookingId;
    }

    public boolean hasDetectedBooking() {
        return detectedBookingNumber != null
                && !detectedBookingNumber.isBlank();
    }

    public boolean isIdentificationRequest() {
        return identificationRequest;
    }

    public boolean isIdentificationSubmitted() {
        return identificationSubmitted;
    }
}
