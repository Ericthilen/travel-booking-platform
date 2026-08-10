package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.AgentChatReplyRequest;
import com.ericthilen.travelbookingplatform.model.CustomerChatConversation;
import com.ericthilen.travelbookingplatform.model.CustomerChatMessage;
import com.ericthilen.travelbookingplatform.model.CustomerChatStatus;
import com.ericthilen.travelbookingplatform.model.EmailTicket;
import com.ericthilen.travelbookingplatform.model.EmailTicketStatus;
import com.ericthilen.travelbookingplatform.model.Role;
import com.ericthilen.travelbookingplatform.model.User;
import com.ericthilen.travelbookingplatform.repository.UserRepository;
import com.ericthilen.travelbookingplatform.service.CustomerChatRealtimeService;
import com.ericthilen.travelbookingplatform.service.CustomerChatService;
import com.ericthilen.travelbookingplatform.service.CustomerSatisfactionService;
import com.ericthilen.travelbookingplatform.service.CustomerSupportKnowledgeBase;
import com.ericthilen.travelbookingplatform.service.EmailTemplateService;
import com.ericthilen.travelbookingplatform.service.EmailTicketService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
public class SupportAgentController {

    private final CustomerChatService customerChatService;
    private final EmailTicketService emailTicketService;
    private final EmailTemplateService emailTemplateService;
    private final CustomerChatRealtimeService realtimeService;
    private final CustomerSatisfactionService satisfactionService;
    private final CustomerSupportKnowledgeBase knowledgeBase;
    private final UserRepository userRepository;

    public SupportAgentController(
            CustomerChatService customerChatService,
            EmailTicketService emailTicketService,
            EmailTemplateService emailTemplateService,
            CustomerChatRealtimeService realtimeService,
            CustomerSatisfactionService satisfactionService,
            CustomerSupportKnowledgeBase knowledgeBase,
            UserRepository userRepository
    ) {
        this.customerChatService = customerChatService;
        this.emailTicketService = emailTicketService;
        this.emailTemplateService = emailTemplateService;
        this.realtimeService = realtimeService;
        this.satisfactionService = satisfactionService;
        this.knowledgeBase = knowledgeBase;
        this.userRepository = userRepository;
    }

    @GetMapping("/kundtjanst")
    public String dashboard(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(defaultValue = "") String q,
            Model model,
            Authentication authentication
    ) {
        String agentEmail = agentEmail(authentication);
        String agentName = agentDisplayName(authentication);
        var conversations = filterConversations(mine
                ? customerChatService.getConversationsForAgent(agentEmail)
                : customerChatService.getConversations(status), q);
        var myChats = customerChatService.getConversationsForAgent(agentEmail);
        var myEmails = emailTicketService.getTicketsForAgent(agentEmail);

        model.addAttribute(
                "conversations",
                conversations
        );
        model.addAttribute(
                "queueOverview",
                customerChatService.getAgentQueueOverview()
        );
        model.addAttribute("statuses", CustomerChatStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("mine", mine);
        model.addAttribute("myAssignedCount", myChats.size() + myEmails.size());
        model.addAttribute("myEmailConversations", myEmails);
        model.addAttribute("agentName", agentName);
        model.addAttribute("searchQuery", q);
        model.addAttribute("supportAgentNames", supportAgentNames());
        model.addAttribute("customerSatisfactionAverage", satisfactionService.averageRating());
        model.addAttribute("customerSatisfactionCount", satisfactionService.reviewCount());

        return "support-agent-dashboard";
    }

    @GetMapping("/kundtjanst/api/chattar")
    @org.springframework.web.bind.annotation.ResponseBody
    public AgentDashboardResponse dashboardData(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(defaultValue = "") String q,
            Authentication authentication
    ) {
        String agentEmail = agentEmail(authentication);
        var conversations = filterConversations(mine
                ? customerChatService.getConversationsForAgent(agentEmail)
                : customerChatService.getConversations(status), q);
        var emailTickets = filterEmailTickets(mine
                ? emailTicketService.getTicketsForAgent(agentEmail)
                : emailTicketService.getTickets("ALL"), q);

        return new AgentDashboardResponse(
                customerChatService.getAgentQueueOverview(),
                conversations
                        .stream()
                        .map(this::toAgentConversationResponse)
                        .toList(),
                emailTickets
                        .stream()
                        .map(this::toAgentEmailConversationResponse)
                        .toList(),
                satisfactionService.averageRating(),
                satisfactionService.reviewCount()
        );
    }

    @GetMapping("/kundtjanst/kundnojdhet")
    public String customerSatisfaction(Model model) {
        model.addAttribute("reviews", satisfactionService.getReviews());
        model.addAttribute("averageRating", satisfactionService.averageRating());
        model.addAttribute("reviewCount", satisfactionService.reviewCount());

        return "support-agent-satisfaction";
    }

    @GetMapping("/kundtjanst/kunskapsbas")
    public String knowledgeBase(Model model) {
        model.addAttribute("topics", knowledgeBase.topics());
        model.addAttribute("articles", knowledgeBase.articles());

        return "support-agent-knowledge-base";
    }

    @GetMapping("/kundtjanst/api/chattar/stream")
    @org.springframework.web.bind.annotation.ResponseBody
    public SseEmitter dashboardStream() {
        return realtimeService.subscribeToDashboard();
    }

    @GetMapping("/kundtjanst/chattar/{conversationId}")
    public String chat(
            @PathVariable Long conversationId,
            Model model,
            Authentication authentication
    ) {
        model.addAttribute(
                "conversation",
                customerChatService.getAdminConversation(conversationId)
        );
        model.addAttribute(
                "replyRequest",
                new AgentChatReplyRequest()
        );
        model.addAttribute(
                "currentAgentName",
                agentDisplayName(authentication)
        );

        return "support-agent-chat";
    }

    @GetMapping("/kundtjanst/rapporter")
    public String reports(Model model) {
        model.addAttribute("tickets", customerChatService.getTickets());

        return "support-agent-reports";
    }

    @GetMapping("/kundtjanst/mejl")
    public String emailTickets(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Long ticketId,
            Model model,
            Authentication authentication
    ) {
        var emailTickets = filterEmailTickets(
                emailTicketService.getTickets(status),
                q
        );
        var selectedTicket = ticketId == null
                ? emailTickets.stream().findFirst().orElse(null)
                : emailTicketService.getTicket(ticketId);

        String agentName = agentDisplayName(authentication);
        String agentInitials = "";
        if (agentName != null && !agentName.isBlank()) {
            String[] parts = agentName.split("\\s+");
            if (parts.length > 1) {
                agentInitials = (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
            } else {
                agentInitials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            }
        }

        model.addAttribute("emailTickets", emailTickets);
        model.addAttribute("selectedTicket", selectedTicket);
        model.addAttribute("emailStatuses", EmailTicketStatus.values());
        model.addAttribute("emailTemplates", emailTemplateService.getTemplates());
        model.addAttribute("selectedEmailStatus", status);
        model.addAttribute("emailSearchQuery", q);
        model.addAttribute("agentName", agentName);
        model.addAttribute("agentInitials", agentInitials);
        model.addAttribute("agentEmail", agentEmail(authentication));

        return "support-agent-email-tickets";
    }

    @GetMapping("/kundtjanst/mejl/{ticketId}")
    public String emailTicket(
            @PathVariable Long ticketId,
            Model model
    ) {
        model.addAttribute("ticket", emailTicketService.getTicket(ticketId));
        model.addAttribute("emailStatuses", EmailTicketStatus.values());

        return "support-agent-email-ticket";
    }

    @PostMapping("/kundtjanst/mejl/inkommande")
    public String incomingEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String subject,
            @RequestParam String message,
            RedirectAttributes redirectAttributes
    ) {
        String ticketNumber = emailTicketService
                .createIncomingEmail(email, name, subject, message)
                .getTicketNumber();
        redirectAttributes.addFlashAttribute(
                "chatMessage",
                "Mejlet har skapats som ticket " + ticketNumber + "."
        );

        return "redirect:/kundtjanst/mejl";
    }

    @PostMapping("/kundtjanst/mejl/mallar")
    public String createEmailTemplate(
            @RequestParam String name,
            @RequestParam String content,
            RedirectAttributes redirectAttributes
    ) {
        try {
            emailTemplateService.createTemplate(name, content);
            redirectAttributes.addFlashAttribute(
                    "chatMessage",
                    "Mallen har sparats."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "chatError",
                    exception.getMessage()
            );
        }

        return "redirect:/kundtjanst/mejl";
    }

    @PostMapping("/kundtjanst/mejl/skapa")
    public String createOutgoingEmail(
            @RequestParam String recipientEmail,
            @RequestParam(defaultValue = "") String subject,
            @RequestParam String message,
            @RequestParam(required = false) List<MultipartFile> attachments,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        try {
            var ticket = emailTicketService.createOutgoingEmail(
                    recipientEmail,
                    subject,
                    message,
                    agentEmail(authentication),
                    attachments == null ? List.of() : attachments
            );
            redirectAttributes.addFlashAttribute(
                    "chatMessage",
                    "Mejlet har skapats och skickats."
            );

            return redirectToEmailTicket(ticket.getId());
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "chatError",
                    exception.getMessage()
            );
            return "redirect:/kundtjanst/mejl";
        }
    }

    @PostMapping("/kundtjanst/mejl/{ticketId}/svara")
    public String replyToEmailTicket(
            @PathVariable Long ticketId,
            @RequestParam String message,
            @RequestParam(required = false) List<MultipartFile> attachments,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        try {
            emailTicketService.reply(
                    ticketId,
                    agentEmail(authentication),
                    message,
                    attachments == null ? List.of() : attachments
            );
            redirectAttributes.addFlashAttribute(
                    "chatMessage",
                    "Svaret har skickats och lagts på ärendet."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "chatError",
                    exception.getMessage()
            );
        }

        return redirectToEmailTicket(ticketId);
    }

    @PostMapping("/kundtjanst/mejl/{ticketId}/agent")
    public String assignEmailTicket(
            @PathVariable Long ticketId,
            @RequestParam String agentName,
            @RequestParam(defaultValue = "false") boolean dashboard,
            RedirectAttributes redirectAttributes
    ) {
        emailTicketService.assignAgent(ticketId, agentName);
        redirectAttributes.addFlashAttribute(
                "chatMessage",
                "Agenten har kopplats till ärendet."
        );

        if (dashboard) {
            return "redirect:/kundtjanst";
        }

        return redirectToEmailTicket(ticketId);
    }

    @PostMapping("/kundtjanst/mejl/{ticketId}/status")
    public String updateEmailTicketStatus(
            @PathVariable Long ticketId,
            @RequestParam EmailTicketStatus status,
            @RequestParam(defaultValue = "false") boolean dashboard,
            RedirectAttributes redirectAttributes
    ) {
        emailTicketService.updateStatus(ticketId, status);
        redirectAttributes.addFlashAttribute(
                "chatMessage",
                "Status har uppdaterats."
        );

        if (dashboard) {
            return "redirect:/kundtjanst";
        }

        return redirectToEmailTicket(ticketId);
    }

    @GetMapping("/kundtjanst/api/chattar/{conversationId}")
    @org.springframework.web.bind.annotation.ResponseBody
    public AgentChatResponse chatData(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        CustomerChatConversation conversation =
                customerChatService.getAdminConversation(conversationId);
        String currentAgentName = agentDisplayName(authentication);

        return new AgentChatResponse(
                conversation.getId(),
                conversation.getSubject(),
                conversation.getStatus().name(),
                conversation.getStatus().getDisplayName(),
                conversation.hasAgentJoined(),
                conversation.getAssignedAgentName(),
                conversation
                        .getMessages()
                        .stream()
                        .map(message -> toAgentMessageResponse(
                                message,
                                currentAgentName
                        ))
                        .toList()
        );
    }

    @GetMapping("/kundtjanst/api/chattar/{conversationId}/stream")
    @org.springframework.web.bind.annotation.ResponseBody
    public SseEmitter chatStream(@PathVariable Long conversationId) {
        CustomerChatConversation conversation =
                customerChatService.getAdminConversation(conversationId);

        return realtimeService.subscribeToConversation(conversation.getPublicId());
    }

    @PostMapping("/kundtjanst/api/chattar/{conversationId}/typing")
    @org.springframework.web.bind.annotation.ResponseBody
    public void typing(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "false") boolean active,
            @RequestParam(defaultValue = "") String preview,
            Authentication authentication
    ) {
        CustomerChatConversation conversation =
                customerChatService.getAdminConversation(conversationId);

        realtimeService.typing(
                conversation.getPublicId(),
                "AGENT",
                agentDisplayName(authentication),
                active,
                preview
        );
    }

    @PostMapping("/kundtjanst/chattar/{conversationId}/anslut")
    public String joinChat(
            @PathVariable Long conversationId,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        customerChatService.joinConversation(
                conversationId,
                agentEmail(authentication)
        );
        redirectAttributes.addFlashAttribute(
                "chatMessage",
                "Du är nu ansluten till kundens chatt."
        );

        return redirectToChat(conversationId);
    }

    @PostMapping("/kundtjanst/chattar/{conversationId}/svara")
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
                    agentEmail(authentication)
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

    @PostMapping("/kundtjanst/chattar/{conversationId}/meddelanden/{messageId}/redigera")
    @org.springframework.web.bind.annotation.ResponseBody
    public AgentChatResponse editReply(
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @RequestParam String message,
            Authentication authentication
    ) {
        CustomerChatConversation conversation =
                customerChatService.editAgentReply(
                        conversationId,
                        messageId,
                        message,
                        agentEmail(authentication)
                );
        String currentAgentName = agentDisplayName(authentication);

        return new AgentChatResponse(
                conversation.getId(),
                conversation.getSubject(),
                conversation.getStatus().name(),
                conversation.getStatus().getDisplayName(),
                conversation.hasAgentJoined(),
                conversation.getAssignedAgentName(),
                conversation
                        .getMessages()
                        .stream()
                        .map(chatMessage -> toAgentMessageResponse(
                                chatMessage,
                                currentAgentName
                        ))
                        .toList()
        );
    }

    @PostMapping("/kundtjanst/chattar/{conversationId}/anteckning")
    public String note(
            @PathVariable Long conversationId,
            @RequestParam String note,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        try {
            customerChatService.addAgentNote(
                    conversationId,
                    note,
                    agentEmail(authentication)
            );
            redirectAttributes.addFlashAttribute(
                    "chatMessage",
                    "Anteckningen har sparats."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "chatError",
                    exception.getMessage()
            );
        }

        return redirectToChat(conversationId);
    }

    @PostMapping("/kundtjanst/chattar/{conversationId}/avsluta")
    public String close(
            @PathVariable Long conversationId,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        customerChatService.closeConversation(
                conversationId,
                agentEmail(authentication)
        );
        redirectAttributes.addFlashAttribute(
                "chatMessage",
                "Chatten har avslutats."
        );

        return redirectToChat(conversationId);
    }

    @PostMapping("/kundtjanst/chattar/{conversationId}/eskalera")
    public String escalate(
            @PathVariable Long conversationId,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        customerChatService.escalateToTicket(
                conversationId,
                agentEmail(authentication)
        );
        redirectAttributes.addFlashAttribute(
                "chatMessage",
                "Chatten har eskalerats och finns nu under Rapporter."
        );

        return "redirect:/kundtjanst/rapporter";
    }

    @PostMapping("/kundtjanst/chattar/{conversationId}/arkivera")
    public String archive(
            @PathVariable Long conversationId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            customerChatService.archiveConversation(conversationId);
            redirectAttributes.addFlashAttribute(
                    "chatMessage",
                    "Chatten har arkiverats."
            );
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "chatError",
                    exception.getMessage()
            );
        }

        return "redirect:/kundtjanst";
    }

    private String redirectToChat(Long conversationId) {
        return "redirect:/kundtjanst/chattar/" + conversationId;
    }

    private String redirectToEmailTicket(Long ticketId) {
        return "redirect:/kundtjanst/mejl?ticketId=" + ticketId;
    }

    private String agentEmail(Authentication authentication) {
        if (authentication == null) {
            return "Agent";
        }

        return authentication.getName();
    }

    private String agentDisplayName(Authentication authentication) {
        String email = agentEmail(authentication);

        if (!email.contains("@")) {
            return email;
        }

        String name = email.substring(0, email.indexOf("@"))
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ");

        if (name.isBlank()) {
            return "Kundtjänst";
        }

        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private List<String> supportAgentNames() {
        return userRepository
                .findAll()
                .stream()
                .filter(user -> user.getRole() == Role.ROLE_AGENT
                        || user.getRole() == Role.ROLE_CEO)
                .map(User::getFullName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private AgentConversationResponse toAgentConversationResponse(
            CustomerChatConversation conversation
    ) {
        return new AgentConversationResponse(
                conversation.getId(),
                "C-" + conversation.getId(),
                "CHAT",
                "/kundtjanst/chattar/" + conversation.getId(),
                conversation.getSubject(),
                conversation.getCustomerName(),
                conversation.getCustomerEmail(),
                conversation.getStatus().name(),
                conversation.getStatus().getDisplayName(),
                conversation.hasAgentJoined(),
                conversation.getAssignedAgentName(),
                conversation
                        .getUpdatedAt()
                        .format(DateTimeFormatter.ofPattern("HH:mm")),
                archiveAt(conversation)
        );
    }

    private AgentConversationResponse toAgentEmailConversationResponse(
            EmailTicket ticket
    ) {
        return new AgentConversationResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                "EMAIL",
                "/kundtjanst/mejl?ticketId=" + ticket.getId(),
                ticket.getSubject(),
                ticket.getCustomerName(),
                ticket.getCustomerEmail(),
                ticket.getStatus().name(),
                ticket.getStatus().getDisplayName(),
                ticket.getAssignedAgentName() != null
                        && !ticket.getAssignedAgentName().isBlank(),
                ticket.getAssignedAgentName(),
                ticket
                        .getUpdatedAt()
                        .format(DateTimeFormatter.ofPattern("HH:mm")),
                ""
        );
    }

    private List<CustomerChatConversation> filterConversations(
            List<CustomerChatConversation> conversations,
            String query
    ) {
        String normalizedQuery = normalizeSearch(query);

        if (normalizedQuery.isBlank()) {
            return conversations;
        }

        return conversations
                .stream()
                .filter(conversation ->
                        contains(conversation.getCustomerName(), normalizedQuery)
                                || contains(conversation.getCustomerEmail(), normalizedQuery)
                                || contains(conversation.getSubject(), normalizedQuery)
                                || contains("C-" + conversation.getId(), normalizedQuery)
                                || contains(String.valueOf(conversation.getId()), normalizedQuery)
                )
                .toList();
    }

    private List<EmailTicket> filterEmailTickets(
            List<EmailTicket> tickets,
            String query
    ) {
        String normalizedQuery = normalizeSearch(query);

        if (normalizedQuery.isBlank()) {
            return tickets;
        }

        return tickets
                .stream()
                .filter(ticket ->
                        contains(ticket.getCustomerName(), normalizedQuery)
                                || contains(ticket.getCustomerEmail(), normalizedQuery)
                                || contains(ticket.getSubject(), normalizedQuery)
                                || contains(ticket.getTicketNumber(), normalizedQuery)
                                || contains(String.valueOf(ticket.getId()), normalizedQuery)
                )
                .toList();
    }

    private String normalizeSearch(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String value, String query) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String archiveAt(CustomerChatConversation conversation) {
        if (conversation.getArchiveAt() == null) {
            return "";
        }

        return conversation
                .getArchiveAt()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private AgentMessageResponse toAgentMessageResponse(
            CustomerChatMessage message,
            String currentAgentName
    ) {
        return new AgentMessageResponse(
                message.getId(),
                message.getSender().name(),
                message.getAuthorName(),
                message.getMessage(),
                message
                        .getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                message.isEdited(),
                message.getEditedAt() == null
                        ? ""
                        : message
                                .getEditedAt()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                "AGENT".equals(message.getSender().name())
                        && message.getAuthorName().equals(currentAgentName)
        );
    }

    public record AgentDashboardResponse(
            CustomerChatService.AgentQueueOverview queueOverview,
            List<AgentConversationResponse> conversations,
            List<AgentConversationResponse> emailConversations,
            double customerSatisfactionAverage,
            long customerSatisfactionCount
    ) {
    }

    public record AgentConversationResponse(
            Long id,
            String caseNumber,
            String channel,
            String actionUrl,
            String subject,
            String customerName,
            String customerEmail,
            String status,
            String statusLabel,
            boolean agentJoined,
            String assignedAgentName,
            String updatedAt,
            String archiveAt
    ) {
    }

    public record AgentChatResponse(
            Long id,
            String subject,
            String status,
            String statusLabel,
            boolean agentJoined,
            String assignedAgentName,
            List<AgentMessageResponse> messages
    ) {
    }

    public record AgentMessageResponse(
            Long id,
            String sender,
            String author,
            String message,
            String createdAt,
            boolean edited,
            String editedAt,
            boolean editable
    ) {
    }
}
