package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.EmailTicket;
import com.ericthilen.travelbookingplatform.model.EmailTicketMessage;
import com.ericthilen.travelbookingplatform.model.EmailTicketStatus;
import com.ericthilen.travelbookingplatform.repository.EmailTicketRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

@Service
public class EmailTicketService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmailTicketService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailTicketRepository ticketRepository;
    private final CustomerChatRealtimeService realtimeService;
    private final CustomerSatisfactionService satisfactionService;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String senderAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public EmailTicketService(
            EmailTicketRepository ticketRepository,
            CustomerChatRealtimeService realtimeService,
            CustomerSatisfactionService satisfactionService,
            JavaMailSender mailSender
    ) {
        this.ticketRepository = ticketRepository;
        this.realtimeService = realtimeService;
        this.satisfactionService = satisfactionService;
        this.mailSender = mailSender;
    }

    @Transactional
    public EmailTicket createIncomingEmail(
            String customerEmail,
            String customerName,
            String subject,
            String message
    ) {
        return createIncomingEmail(
                null,
                customerEmail,
                customerName,
                subject,
                message
        );
    }

    @Transactional
    public EmailTicket createIncomingEmail(
            String sourceMessageId,
            String customerEmail,
            String customerName,
            String subject,
            String message
    ) {
        String cleanSourceMessageId = clean(sourceMessageId);

        if (!cleanSourceMessageId.isBlank()
                && ticketRepository.existsBySourceMessageId(cleanSourceMessageId)) {
            return null;
        }

        EmailTicket ticket = ticketRepository.save(new EmailTicket(
                generateTicketNumber(),
                cleanSourceMessageId.isBlank() ? null : cleanSourceMessageId,
                clean(customerEmail),
                clean(customerName),
                cleanSubject(subject),
                cleanMessage(message)
        ));
        realtimeService.notifyDashboard();

        return ticket;
    }

    public List<EmailTicket> getTickets(String status) {
        if (status == null || status.isBlank() || "ALL".equals(status)) {
            return ticketRepository.findAllByOrderByUpdatedAtDesc();
        }

        return ticketRepository.findAllByStatusOrderByUpdatedAtDesc(
                EmailTicketStatus.valueOf(status)
        );
    }

    public List<EmailTicket> getTicketsForAgent(String agentEmail) {
        return ticketRepository
                .findAllByAssignedAgentNameIgnoreCaseOrderByUpdatedAtDesc(
                        agentName(agentEmail)
                );
    }

    public EmailTicket getTicket(Long id) {
        return ticketRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mejlärendet kunde inte hittas."
                ));
    }

    @Transactional
    public EmailTicket assignAgent(Long id, String agentName) {
        EmailTicket ticket = getTicket(id);
        ticket.assignAgent(
                clean(agentName).isBlank() ? null : agentName(agentName)
        );
        EmailTicket saved = ticketRepository.save(ticket);
        realtimeService.notifyDashboard();

        return saved;
    }

    @Transactional
    public EmailTicket reply(Long id, String agentName, String message) {
        return reply(id, agentName, message, List.of());
    }

    @Transactional
    public EmailTicket reply(
            Long id,
            String agentName,
            String message,
            List<MultipartFile> attachments
    ) {
        EmailTicket ticket = getTicket(id);
        String cleanMessage = cleanMessage(message);

        ticket.addMessage(new EmailTicketMessage(
                "AGENT",
                agentName(agentName),
                cleanMessage
        ));
        ticket.updateStatus(EmailTicketStatus.WAITING);
        EmailTicket saved = ticketRepository.save(ticket);
        sendEmail(
                saved.getCustomerEmail(),
                "Svar från EriGo Travel - ärende " + saved.getTicketNumber(),
                createReplyBody(saved, cleanMessage),
                attachments
        );
        realtimeService.notifyDashboard();

        return saved;
    }

    @Transactional
    public EmailTicket createOutgoingEmail(
            String recipientEmail,
            String subject,
            String message,
            String agentEmail,
            List<MultipartFile> attachments
    ) {
        String cleanRecipientEmail = clean(recipientEmail);
        String cleanSubject = cleanSubject(subject);
        String cleanMessage = cleanMessage(message);
        String cleanAgentName = agentName(agentEmail);

        if (cleanRecipientEmail.isBlank()) {
            throw new IllegalArgumentException("Fyll i mottagarens e-post.");
        }

        EmailTicket ticket = ticketRepository.save(EmailTicket.outgoing(
                generateTicketNumber(),
                cleanRecipientEmail,
                cleanSubject,
                cleanMessage,
                cleanAgentName
        ));
        sendEmail(
                cleanRecipientEmail,
                cleanSubject,
                cleanMessage,
                attachments
        );
        realtimeService.notifyDashboard();

        return ticket;
    }

    @Transactional
    public EmailTicket updateStatus(Long id, EmailTicketStatus status) {
        EmailTicket ticket = getTicket(id);
        EmailTicketStatus previousStatus = ticket.getStatus();
        ticket.updateStatus(status);
        EmailTicket saved = ticketRepository.save(ticket);

        if (status == EmailTicketStatus.CLOSED
                && previousStatus != EmailTicketStatus.CLOSED) {
            satisfactionService.sendForEmailTicket(saved);
        }

        realtimeService.notifyDashboard();

        return saved;
    }

    public long countActiveEmailTickets() {
        return ticketRepository.countByStatusNot(EmailTicketStatus.CLOSED);
    }

    private String generateTicketNumber() {
        String ticketNumber;

        do {
            ticketNumber = String.format(
                    Locale.ROOT,
                    "%07d",
                    RANDOM.nextInt(10_000_000)
            );
        } while (ticketRepository.existsByTicketNumber(ticketNumber));

        return ticketNumber;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanSubject(String subject) {
        String cleanSubject = clean(subject);

        if (cleanSubject.isBlank()) {
            return "Nytt mejl till EriGo Travel";
        }

        return cleanSubject.length() > 220
                ? cleanSubject.substring(0, 217) + "..."
                : cleanSubject;
    }

    private String cleanMessage(String message) {
        if (message == null || message.trim().isBlank()) {
            throw new IllegalArgumentException("Skriv ett meddelande först.");
        }

        // We allow HTML now, but let's keep it trimmed
        return message.trim();
    }

    private String agentName(String agentEmail) {
        if (agentEmail == null || agentEmail.isBlank()) {
            return "Kundtjänst";
        }

        String name = agentEmail.split("@")[0]
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ")
                .trim();

        if (name.isBlank()) {
            return "Kundtjänst";
        }

        return Character.toUpperCase(name.charAt(0))
                + name.substring(1).toLowerCase(Locale.ROOT);
    }

    private void sendEmail(
            String recipientEmail,
            String subject,
            String messageText,
            List<MultipartFile> attachments
    ) {
        if (!mailEnabled || senderAddress == null || senderAddress.isBlank()) {
            LOGGER.info(
                    "Mejlet sparades, men skickades inte eftersom "
                            + "e-post inte är konfigurerat. Mottagare: {}",
                    recipientEmail
            );
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    hasAttachments(attachments),
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(senderAddress, "EriGo Travel Kundtjänst");
            helper.setTo(recipientEmail);
            helper.setSubject(subject);

            boolean isHtml = messageText.contains("<table") || messageText.contains("<div") || messageText.contains("<br") || messageText.contains("<p");
            helper.setText(messageText, isHtml);

            if (attachments != null) {
                for (MultipartFile attachment : attachments) {
                    if (attachment != null && !attachment.isEmpty()) {
                        helper.addAttachment(
                                attachment.getOriginalFilename() == null
                                        ? "bilaga"
                                        : attachment.getOriginalFilename(),
                                attachment
                        );
                    }
                }
            }

            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            LOGGER.warn(
                    "Mejlet kunde inte skickas till {}. "
                            + "Det är sparat i ärendet.",
                    recipientEmail,
                    exception
            );
        } catch (Exception exception) {
            LOGGER.warn(
                    "Ett oväntat fel uppstod när mejl skulle skickas "
                            + "till {}. Mejlet är sparat i ärendet.",
                    recipientEmail,
                    exception
            );
        }
    }

    private boolean hasAttachments(List<MultipartFile> attachments) {
        return attachments != null
                && attachments
                        .stream()
                        .anyMatch(file -> file != null && !file.isEmpty());
    }

    private String createReplyBody(EmailTicket ticket, String messageText) {
        if (messageText.contains("<table") || messageText.contains("<div") || messageText.contains("<br") || messageText.contains("<p")) {
            return messageText;
        }
        return messageText.replace("\n", "<br>")
                + "<br><br>---<br>"
                + "EriGo Travel Kundtjänst<br>"
                + "Ärendenummer: "
                + ticket.getTicketNumber();
    }
}
