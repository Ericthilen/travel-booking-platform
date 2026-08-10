package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.EmailTicket;
import com.ericthilen.travelbookingplatform.model.EmailTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailTicketRepository extends JpaRepository<EmailTicket, Long> {

    boolean existsByTicketNumber(String ticketNumber);

    boolean existsBySourceMessageId(String sourceMessageId);

    Optional<EmailTicket> findByTicketNumber(String ticketNumber);

    List<EmailTicket> findAllByOrderByUpdatedAtDesc();

    List<EmailTicket> findAllByAssignedAgentNameIgnoreCaseOrderByUpdatedAtDesc(
            String assignedAgentName
    );

    List<EmailTicket> findAllByStatusOrderByUpdatedAtDesc(EmailTicketStatus status);

    long countByStatusNot(EmailTicketStatus status);
}
