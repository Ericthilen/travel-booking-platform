package com.ericthilen.travelbookingplatform.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customer_chat_conversations")
public class CustomerChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String publicId;

    @Column(length = 160)
    private String customerName;

    @Column(length = 160)
    private String customerEmail;

    @Column(length = 160)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerChatStatus status = CustomerChatStatus.OPEN;

    @Column(nullable = false)
    private boolean customerStarted = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime escalatedAt;

    @Column(length = 160)
    private String assignedAgentName;

    private LocalDateTime agentJoinedAt;

    private LocalDateTime closedAt;

    private Long linkedBookingId;

    @Column(length = 80)
    private String linkedBookingNumber;

    @Column(length = 240)
    private String linkedBookingLabel;

    @Column(length = 180)
    private String bookingRegisterQuery;

    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("createdAt asc")
    private List<CustomerChatMessage> messages = new ArrayList<>();

    public CustomerChatConversation() {
    }

    public CustomerChatConversation(
            String customerName,
            String customerEmail
    ) {
        this.publicId = UUID.randomUUID().toString();
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.subject = "Ny kundchatt";
        this.status = CustomerChatStatus.OPEN;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void addMessage(CustomerChatMessage message) {
        message.setConversation(this);
        messages.add(message);
        updatedAt = message.getCreatedAt();
    }

    public void updateSubject(String subject) {
        if (subject != null && !subject.isBlank()) {
            this.subject = subject;
        }
    }

    public void markCustomerStarted() {
        customerStarted = true;
    }

    public void escalate() {
        if (status != CustomerChatStatus.CLOSED) {
            status = CustomerChatStatus.ESCALATED;
            escalatedAt = LocalDateTime.now();
            updatedAt = escalatedAt;
        }
    }

    public void waitForAgent() {
        if (status != CustomerChatStatus.CLOSED
                && status != CustomerChatStatus.ESCALATED) {
            status = CustomerChatStatus.WAITING_FOR_AGENT;
            updatedAt = LocalDateTime.now();
        }
    }

    public void close() {
        status = CustomerChatStatus.CLOSED;
        closedAt = LocalDateTime.now();
        updatedAt = closedAt;
    }

    public void reopen() {
        if (status == CustomerChatStatus.CLOSED) {
            status = CustomerChatStatus.OPEN;
            closedAt = null;
            updatedAt = LocalDateTime.now();
        }
    }

    public void archiveNow(int archiveAfterMinutes) {
        if (status == CustomerChatStatus.CLOSED) {
            closedAt = LocalDateTime.now().minusMinutes(archiveAfterMinutes);
            updatedAt = LocalDateTime.now();
        }
    }

    public boolean hasAgentJoined() {
        return assignedAgentName != null && !assignedAgentName.isBlank();
    }

    public void assignAgent(String agentName) {
        if (!hasAgentJoined()) {
            assignedAgentName = agentName;
            agentJoinedAt = LocalDateTime.now();
            if (status == CustomerChatStatus.WAITING_FOR_AGENT
                    || status == CustomerChatStatus.ESCALATED) {
                status = CustomerChatStatus.OPEN;
            }
            updatedAt = agentJoinedAt;
        }
    }

    public void linkBooking(
            Long bookingId,
            String bookingNumber,
            String bookingLabel,
            String registerQuery
    ) {
        this.linkedBookingId = bookingId;
        this.linkedBookingNumber = bookingNumber;
        this.linkedBookingLabel = bookingLabel;
        this.bookingRegisterQuery = registerQuery;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getSubject() {
        return subject;
    }

    public CustomerChatStatus getStatus() {
        return status;
    }

    public boolean isCustomerStarted() {
        return customerStarted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public String getAssignedAgentName() {
        return assignedAgentName;
    }

    public LocalDateTime getAgentJoinedAt() {
        return agentJoinedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public Long getLinkedBookingId() {
        return linkedBookingId;
    }

    public String getLinkedBookingNumber() {
        return linkedBookingNumber;
    }

    public String getLinkedBookingLabel() {
        return linkedBookingLabel;
    }

    public String getBookingRegisterQuery() {
        return bookingRegisterQuery;
    }

    public boolean hasLinkedBooking() {
        return bookingRegisterQuery != null && !bookingRegisterQuery.isBlank();
    }

    public LocalDateTime getArchiveAt() {
        if (closedAt == null) {
            return null;
        }

        return closedAt.plusMinutes(15);
    }

    public List<CustomerChatMessage> getMessages() {
        return messages;
    }
}
