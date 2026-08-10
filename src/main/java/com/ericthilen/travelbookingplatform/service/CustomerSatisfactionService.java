package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.CustomerChatConversation;
import com.ericthilen.travelbookingplatform.model.CustomerSatisfactionCaseType;
import com.ericthilen.travelbookingplatform.model.CustomerSatisfactionReview;
import com.ericthilen.travelbookingplatform.model.EmailTicket;
import com.ericthilen.travelbookingplatform.repository.CustomerSatisfactionReviewRepository;
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

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
public class CustomerSatisfactionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CustomerSatisfactionService.class);

    private final CustomerSatisfactionReviewRepository reviewRepository;
    private final JavaMailSender mailSender;
    private final CustomerChatRealtimeService realtimeService;

    @Value("${app.mail.from:}")
    private String senderAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.survey.base-url:}")
    private String surveyBaseUrl;

    public CustomerSatisfactionService(
            CustomerSatisfactionReviewRepository reviewRepository,
            JavaMailSender mailSender,
            CustomerChatRealtimeService realtimeService
    ) {
        this.reviewRepository = reviewRepository;
        this.mailSender = mailSender;
        this.realtimeService = realtimeService;
    }

    @Transactional
    public void sendForEmailTicket(EmailTicket ticket) {
        String caseNumber = "EMAIL-" + ticket.getTicketNumber();

        if (ticket.getCustomerEmail() == null
                || ticket.getCustomerEmail().isBlank()
                || reviewRepository.existsByCaseNumber(caseNumber)) {
            return;
        }

        CustomerSatisfactionReview review = reviewRepository.save(
                new CustomerSatisfactionReview(
                        CustomerSatisfactionCaseType.EMAIL,
                        caseNumber,
                        ticket.getCustomerEmail(),
                        ticket.getCustomerName(),
                        ticket.getAssignedAgentName(),
                        ticket.getSubject()
                )
        );
        sendSurveyMail(review);
    }

    @Transactional
    public void sendForChat(CustomerChatConversation conversation) {
        String caseNumber = "CHAT-" + conversation.getId();

        if (conversation.getCustomerEmail() == null
                || conversation.getCustomerEmail().isBlank()
                || reviewRepository.existsByCaseNumber(caseNumber)) {
            return;
        }

        CustomerSatisfactionReview review = reviewRepository.save(
                new CustomerSatisfactionReview(
                        CustomerSatisfactionCaseType.CHAT,
                        caseNumber,
                        conversation.getCustomerEmail(),
                        conversation.getCustomerName(),
                        conversation.getAssignedAgentName(),
                        conversation.getSubject()
                )
        );
        sendSurveyMail(review);
    }

    @Transactional
    public CustomerSatisfactionReview submitRating(
            String surveyToken,
            int rating
    ) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Betyget måste vara mellan 1 och 5.");
        }

        CustomerSatisfactionReview review =
                reviewRepository
                        .findBySurveyToken(surveyToken)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Enkäten kunde inte hittas."
                        ));
        review.rate(rating);
        CustomerSatisfactionReview savedReview = reviewRepository.save(review);
        realtimeService.notifyDashboard();

        return savedReview;
    }

    public List<CustomerSatisfactionReview> getReviews() {
        return reviewRepository.findAllByOrderBySentAtDesc();
    }

    public double averageRating() {
        return reviewRepository
                .findAll()
                .stream()
                .filter(review -> review.getRating() != null)
                .mapToInt(CustomerSatisfactionReview::getRating)
                .average()
                .orElse(0);
    }

    public long reviewCount() {
        return reviewRepository
                .findAll()
                .stream()
                .filter(review -> review.getRating() != null)
                .count();
    }

    private void sendSurveyMail(CustomerSatisfactionReview review) {
        if (!mailEnabled || senderAddress == null || senderAddress.isBlank()) {
            LOGGER.info(
                    "Kundnöjdhetsenkät sparades men skickades inte. Ärende: {}",
                    review.getCaseNumber()
            );
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(senderAddress, "EriGo Travel Kundtjänst");
            helper.setTo(review.getCustomerEmail());
            helper.setSubject("Hur gick det med ditt ärende hos EriGo Travel?");
            helper.setText(createSurveyBody(review), true);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            LOGGER.warn(
                    "Kundnöjdhetsenkäten kunde inte skickas för {}.",
                    review.getCaseNumber(),
                    exception
            );
        } catch (Exception exception) {
            LOGGER.warn(
                    "Ett oväntat fel uppstod när kundnöjdhetsenkäten "
                            + "skulle skickas för {}.",
                    review.getCaseNumber(),
                    exception
            );
        }
    }

    private String createSurveyBody(CustomerSatisfactionReview review) {
        StringBuilder ratings = new StringBuilder();
        String ratingBaseUrl = ratingBaseUrl();

        for (int rating = 1; rating <= 5; rating++) {
            ratings
                    .append("<a href=\"")
                    .append(ratingBaseUrl)
                    .append("/kundnojdhet/")
                    .append(review.getSurveyToken())
                    .append("?rating=")
                    .append(rating)
                    .append("\" style=\"display:inline-block;margin:0 6px 10px 0;")
                    .append("padding:14px 18px;border-radius:14px;")
                    .append("background:#064653;color:#ffffff;text-decoration:none;")
                    .append("font-weight:800;\">")
                    .append(rating)
                    .append("</a>");
        }

        return "<div style=\"font-family:Arial,sans-serif;color:#102427;\">"
                + "<h2>Tack för att du kontaktade EriGo Travel</h2>"
                + "<p>Vi vill gärna veta hur du upplevde servicen i ärende "
                + "<strong>" + review.getCaseNumber() + "</strong>.</p>"
                + "<p>Klicka på ett betyg nedan. 1 är sämst och 5 är bäst.</p>"
                + "<div>" + ratings + "</div>"
                + "<p style=\"color:#64767b;\">Enkäten skickas bara en gång per ärende.</p>"
                + "</div>";
    }

    private String ratingBaseUrl() {
        if (surveyBaseUrl != null && !surveyBaseUrl.isBlank()) {
            return removeTrailingSlash(surveyBaseUrl);
        }

        String cleanBaseUrl = removeTrailingSlash(baseUrl);

        if (!cleanBaseUrl.contains("localhost")
                && !cleanBaseUrl.contains("127.0.0.1")) {
            return cleanBaseUrl;
        }

        return localNetworkBaseUrl(cleanBaseUrl);
    }

    private String localNetworkBaseUrl(String fallbackBaseUrl) {
        try {
            URI uri = URI.create(fallbackBaseUrl);
            int port = uri.getPort() == -1 ? 8080 : uri.getPort();

            for (NetworkInterface networkInterface
                    : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp()
                        || networkInterface.isLoopback()
                        || networkInterface.isVirtual()) {
                    continue;
                }

                for (var address
                        : Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address
                            && address.isSiteLocalAddress()) {
                        return uri.getScheme()
                                + "://"
                                + address.getHostAddress()
                                + ":"
                                + port;
                    }
                }
            }
        } catch (Exception exception) {
            LOGGER.info(
                    "Kunde inte hitta lokal nätverksadress för enkätlänk. "
                            + "Använder {}.",
                    fallbackBaseUrl
            );
        }

        return fallbackBaseUrl;
    }

    private String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String cleanValue = value.trim();

        while (cleanValue.endsWith("/")) {
            cleanValue = cleanValue.substring(0, cleanValue.length() - 1);
        }

        return cleanValue;
    }
}
