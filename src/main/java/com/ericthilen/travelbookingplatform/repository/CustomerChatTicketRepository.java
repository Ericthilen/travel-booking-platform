package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.CustomerChatTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerChatTicketRepository
        extends JpaRepository<CustomerChatTicket, Long> {

    List<CustomerChatTicket> findAllByOrderByCreatedAtDesc();

    Optional<CustomerChatTicket> findByConversationId(Long conversationId);
}
