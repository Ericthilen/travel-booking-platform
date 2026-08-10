package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.CustomerChatMessageRequest;
import com.ericthilen.travelbookingplatform.model.CustomerChatConversation;
import com.ericthilen.travelbookingplatform.model.CustomerChatMessage;
import com.ericthilen.travelbookingplatform.model.CustomerChatSender;
import com.ericthilen.travelbookingplatform.service.CustomerChatRealtimeService;
import com.ericthilen.travelbookingplatform.service.CustomerChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/chat")
public class CustomerChatController {

    private final CustomerChatService customerChatService;
    private final CustomerChatRealtimeService realtimeService;

    public CustomerChatController(
            CustomerChatService customerChatService,
            CustomerChatRealtimeService realtimeService
    ) {
        this.customerChatService = customerChatService;
        this.realtimeService = realtimeService;
    }

    @PostMapping("/start")
    public ChatResponse start(Principal principal) {
        CustomerChatConversation conversation =
                customerChatService.startConversation(principal);

        return toResponse(conversation);
    }

    @PostMapping("/{publicId}/messages")
    public ChatResponse message(
            @PathVariable String publicId,
            @Valid @RequestBody CustomerChatMessageRequest request,
            Principal principal
    ) {
        try {
            CustomerChatConversation conversation =
                    customerChatService.addCustomerMessage(
                            publicId,
                            request.getMessage(),
                            principal
                    );

            return toResponse(conversation);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/{publicId}")
    public ChatResponse conversation(
            @PathVariable String publicId,
            Principal principal
    ) {
        try {
            return toResponse(customerChatService.getConversationForCustomer(
                    publicId,
                    principal
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/{publicId}/stream")
    public SseEmitter stream(
            @PathVariable String publicId,
            Principal principal
    ) {
        try {
            customerChatService.getConversationForCustomer(publicId, principal);
            return realtimeService.subscribeToConversation(publicId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/{publicId}/typing")
    public void typing(
            @PathVariable String publicId,
            @RequestParam(defaultValue = "false") boolean active,
            @RequestParam(defaultValue = "") String preview,
            Principal principal
    ) {
        try {
            customerChatService.getConversationForCustomer(publicId, principal);
            realtimeService.typing(
                    publicId,
                    "CUSTOMER",
                    customerName(principal),
                    active,
                    preview
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/{publicId}/close")
    public ChatResponse close(
            @PathVariable String publicId,
            Principal principal
    ) {
        try {
            return toResponse(customerChatService.closeCustomerConversation(
                    publicId,
                    principal
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private ChatResponse toResponse(CustomerChatConversation conversation) {
        return new ChatResponse(
                conversation.getPublicId(),
                conversation.getStatus().name(),
                conversation.getStatus().getDisplayName(),
                conversation
                        .getMessages()
                        .stream()
                        .filter(message ->
                                message.getSender() != CustomerChatSender.NOTE
                        )
                        .map(this::toMessageResponse)
                        .toList()
        );
    }

    private ChatMessageResponse toMessageResponse(CustomerChatMessage message) {
        return new ChatMessageResponse(
                message.getSender().name(),
                message.getAuthorName(),
                message.getMessage(),
                message
                        .getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("HH:mm")),
                message.isEdited()
        );
    }

    private String customerName(Principal principal) {
        if (principal == null) {
            return "Kund";
        }

        return principal.getName();
    }

    public record ChatResponse(
            String publicId,
            String status,
            String statusLabel,
            List<ChatMessageResponse> messages
    ) {
    }

    public record ChatMessageResponse(
            String sender,
            String author,
            String message,
            String time,
            boolean edited
    ) {
    }
}
