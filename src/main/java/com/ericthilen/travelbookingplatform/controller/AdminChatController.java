package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.AgentChatReplyRequest;
import com.ericthilen.travelbookingplatform.model.CustomerChatStatus;
import com.ericthilen.travelbookingplatform.service.CustomerChatService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminChatController {

    private final CustomerChatService customerChatService;

    public AdminChatController(CustomerChatService customerChatService) {
        this.customerChatService = customerChatService;
    }

    @GetMapping("/admin/chattar")
    public String showChats(
            @RequestParam(defaultValue = "ALL") String status,
            Model model
    ) {
        model.addAttribute(
                "conversations",
                customerChatService.getConversations(status)
        );
        model.addAttribute("statuses", CustomerChatStatus.values());
        model.addAttribute("selectedStatus", status);

        return "admin-chats";
    }

    @GetMapping("/admin/chattar/{conversationId}")
    public String showChat(
            @PathVariable Long conversationId,
            Model model
    ) {
        model.addAttribute(
                "conversation",
                customerChatService.getAdminConversation(conversationId)
        );
        model.addAttribute(
                "replyRequest",
                new AgentChatReplyRequest()
        );

        return "admin-chat-details";
    }

    @PostMapping("/admin/chattar/{conversationId}/svara")
    public String reply(
            @PathVariable Long conversationId,
            @Valid
            @ModelAttribute("replyRequest")
            AgentChatReplyRequest replyRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "chatError",
                    "Skriv ett svar innan du skickar."
            );
            return redirectToChat(conversationId);
        }

        try {
            customerChatService.addAgentReply(
                    conversationId,
                    replyRequest.getMessage(),
                    authentication == null ? "Agent" : authentication.getName()
            );
            redirectAttributes.addFlashAttribute(
                    "chatMessage",
                    "Svaret har skickats."
            );
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "chatError",
                    exception.getMessage()
            );
        }

        return redirectToChat(conversationId);
    }

    @PostMapping("/admin/chattar/{conversationId}/anslut")
    public String join(
            @PathVariable Long conversationId,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        customerChatService.joinConversation(
                conversationId,
                authentication == null ? "Agent" : authentication.getName()
        );
        redirectAttributes.addFlashAttribute(
                "chatMessage",
                "Du är nu ansluten till kundens chatt."
        );

        return redirectToChat(conversationId);
    }

    @PostMapping("/admin/chattar/{conversationId}/avsluta")
    public String close(
            @PathVariable Long conversationId,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        customerChatService.closeConversation(
                conversationId,
                authentication == null ? "Agent" : authentication.getName()
        );
        redirectAttributes.addFlashAttribute(
                "chatMessage",
                "Chatten har avslutats."
        );

        return redirectToChat(conversationId);
    }

    private String redirectToChat(Long conversationId) {
        return "redirect:/admin/chattar/" + conversationId;
    }
}
