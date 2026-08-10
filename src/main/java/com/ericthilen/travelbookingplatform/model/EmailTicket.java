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

@Entity
@Table(name = "email_tickets")
public class EmailTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 7)
    private String ticketNumber;

    @Column(unique = true, length = 320)
    private String sourceMessageId;

    @Column(nullable = false, length = 180)
    private String customerEmail;

    @Column(length = 180)
    private String customerName;

    @Column(nullable = false, length = 220)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EmailTicketStatus status = EmailTicketStatus.OPEN;

    @Column(length = 160)
    private String assignedAgentName;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(
            mappedBy = "ticket",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("createdAt asc")
    private List<EmailTicketMessage> messages = new ArrayList<>();

    public EmailTicket() {
    }

    public EmailTicket(
            String ticketNumber,
            String sourceMessageId,
            String customerEmail,
            String customerName,
            String subject,
            String message
    ) {
        this.ticketNumber = ticketNumber;
        this.sourceMessageId = sourceMessageId;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.subject = subject;
        addMessage(new EmailTicketMessage(
                "CUSTOMER",
                customerName == null || customerName.isBlank()
                        ? customerEmail
                        : customerName,
                message
        ));
    }

    public static EmailTicket outgoing(
            String ticketNumber,
            String customerEmail,
            String subject,
            String message,
            String agentName
    ) {
        EmailTicket ticket = new EmailTicket();
        ticket.ticketNumber = ticketNumber;
        ticket.customerEmail = customerEmail;
        ticket.customerName = customerEmail;
        ticket.subject = subject;
        ticket.addMessage(new EmailTicketMessage(
                "AGENT",
                agentName,
                message
        ));

        return ticket;
    }

    public void addMessage(EmailTicketMessage message) {
        message.setTicket(this);
        messages.add(message);
        updatedAt = message.getCreatedAt();
    }

    public void assignAgent(String agentName) {
        assignedAgentName = agentName;
        updatedAt = LocalDateTime.now();
    }

    public void updateStatus(EmailTicketStatus status) {
        this.status = status;
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getSourceMessageId() {
        return sourceMessageId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getSubject() {
        return subject;
    }

    public EmailTicketStatus getStatus() {
        return status;
    }

    public String getAssignedAgentName() {
        return assignedAgentName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<EmailTicketMessage> getMessages() {
        return messages;
    }
}
