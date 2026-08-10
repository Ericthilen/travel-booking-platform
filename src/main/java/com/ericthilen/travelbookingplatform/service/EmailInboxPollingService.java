package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.EmailTicket;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.FlagTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

@Service
public class EmailInboxPollingService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmailInboxPollingService.class);

    private final EmailTicketService emailTicketService;

    @Value("${app.mail.inbound.enabled:true}")
    private boolean inboundEnabled;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${app.mail.inbound.host:imap.gmail.com}")
    private String host;

    @Value("${app.mail.inbound.port:993}")
    private int port;

    @Value("${app.mail.inbound.folder:INBOX}")
    private String folderName;

    @Value("${app.mail.inbound.connection-timeout-ms:3000}")
    private int connectionTimeoutMs;

    @Value("${app.mail.inbound.timeout-ms:3000}")
    private int timeoutMs;

    public EmailInboxPollingService(EmailTicketService emailTicketService) {
        this.emailTicketService = emailTicketService;
    }

    @Scheduled(
            fixedDelayString = "${app.mail.inbound.poll-delay-ms:5000}",
            initialDelayString = "${app.mail.inbound.initial-delay-ms:15000}"
    )
    public void pollInbox() {
        if (!inboundEnabled || username.isBlank() || password.isBlank()) {
            return;
        }

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", host);
        properties.put("mail.imaps.port", String.valueOf(port));
        properties.put("mail.imaps.ssl.enable", "true");
        properties.put("mail.imaps.connectiontimeout", String.valueOf(connectionTimeoutMs));
        properties.put("mail.imaps.timeout", String.valueOf(timeoutMs));

        Session session = Session.getInstance(properties);

        try (Store store = session.getStore("imaps")) {
            store.connect(host, port, username, password);
            readUnreadMessages(store);
        } catch (MessagingException exception) {
            LOGGER.warn(
                    "Kunde inte läsa inkommande mejl från {}. "
                            + "Kontrollera IMAP och app-lösenord.",
                    username,
                    exception
            );
        }
    }

    private void readUnreadMessages(Store store) throws MessagingException {
        Folder folder = store.getFolder(folderName);

        if (folder == null || !folder.exists()) {
            LOGGER.warn("IMAP-mappen {} kunde inte hittas.", folderName);
            return;
        }

        try (folder) {
            folder.open(Folder.READ_WRITE);

            Message[] unreadMessages = folder.search(new FlagTerm(
                    new Flags(Flags.Flag.SEEN),
                    false
            ));

            for (Message message : unreadMessages) {
                handleMessage(message);
            }
        }
    }

    private void handleMessage(Message message) {
        try {
            String sourceMessageId = sourceMessageId(message);
            String customerEmail = senderEmail(message);
            String customerName = senderName(message);
            String subject = message.getSubject();
            String body = extractBody(message);

            EmailTicket ticket = emailTicketService.createIncomingEmail(
                    sourceMessageId,
                    customerEmail,
                    customerName,
                    subject,
                    body
            );

            message.setFlag(Flags.Flag.SEEN, true);

            if (ticket != null) {
                LOGGER.info(
                        "Skapade mejlticket {} från {}.",
                        ticket.getTicketNumber(),
                        customerEmail
                );
            }
        } catch (Exception exception) {
            LOGGER.warn(
                    "Ett inkommande mejl kunde inte omvandlas till ticket.",
                    exception
            );
        }
    }

    private String sourceMessageId(Message message) throws MessagingException {
        String[] ids = message.getHeader("Message-ID");

        if (ids != null && ids.length > 0 && ids[0] != null && !ids[0].isBlank()) {
            return ids[0].trim();
        }

        String fallback = senderEmail(message)
                + "|"
                + safe(message.getSubject())
                + "|"
                + sentAt(message);

        return sha256(fallback);
    }

    private String senderEmail(Message message) throws MessagingException {
        Address[] from = message.getFrom();

        if (from == null || from.length == 0) {
            return username;
        }

        if (from[0] instanceof InternetAddress internetAddress) {
            return internetAddress.getAddress();
        }

        return from[0].toString();
    }

    private String senderName(Message message) throws MessagingException {
        Address[] from = message.getFrom();

        if (from == null || from.length == 0) {
            return "";
        }

        if (from[0] instanceof InternetAddress internetAddress
                && internetAddress.getPersonal() != null) {
            return internetAddress.getPersonal();
        }

        return "";
    }

    private String extractBody(Part part)
            throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            return content == null ? "" : content.toString();
        }

        if (part.isMimeType("text/html")) {
            Object content = part.getContent();
            return content == null ? "" : htmlToText(content.toString());
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            String htmlFallback = "";

            for (int index = 0; index < multipart.getCount(); index++) {
                BodyPart bodyPart = multipart.getBodyPart(index);
                String text = extractBody(bodyPart);

                if (bodyPart.isMimeType("text/plain") && !text.isBlank()) {
                    return text;
                }

                if (htmlFallback.isBlank() && !text.isBlank()) {
                    htmlFallback = text;
                }
            }

            return htmlFallback;
        }

        return "";
    }

    private String sentAt(Message message) throws MessagingException {
        Date sentDate = message.getSentDate();
        return sentDate == null ? "" : String.valueOf(sentDate.getTime());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String htmlToText(String html) {
        return html
                .replaceAll("(?is)<(script|style).*?>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n\\s+", "\n")
                .trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();

            for (byte current : hash) {
                builder.append(String.format("%02x", current));
            }

            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            return String.valueOf(value.hashCode());
        }
    }
}
