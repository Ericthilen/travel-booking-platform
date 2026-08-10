package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.CustomerChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerChatMessageRepository
        extends JpaRepository<CustomerChatMessage, Long> {

    List<CustomerChatMessage> findAllByConversationPublicIdOrderByCreatedAtAsc(
            String publicId
    );
}
