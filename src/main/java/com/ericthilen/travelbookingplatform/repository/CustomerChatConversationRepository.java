package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.CustomerChatConversation;
import com.ericthilen.travelbookingplatform.model.CustomerChatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerChatConversationRepository
        extends JpaRepository<CustomerChatConversation, Long> {

    Optional<CustomerChatConversation> findByPublicId(String publicId);

    List<CustomerChatConversation> findAllByCustomerStartedTrueOrderByUpdatedAtDesc();

    List<CustomerChatConversation> findAllByCustomerStartedTrueAndAssignedAgentNameIgnoreCaseOrderByUpdatedAtDesc(
            String assignedAgentName
    );

    List<CustomerChatConversation> findAllByCustomerStartedTrueAndStatusOrderByUpdatedAtDesc(
            CustomerChatStatus status
    );

    long countByCustomerStartedTrueAndStatus(CustomerChatStatus status);
}
