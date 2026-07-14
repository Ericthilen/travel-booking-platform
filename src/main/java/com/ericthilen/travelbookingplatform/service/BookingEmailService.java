package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.EmailStatus;
import com.ericthilen.travelbookingplatform.model.Invoice;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Service
public class BookingEmailService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BookingEmailService.class);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JavaMailSender mailSender;
    private final InvoicePdfService invoicePdfService;

    @Value("${app.mail.from:}")
    private String senderAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public BookingEmailService(
            JavaMailSender mailSender,
            InvoicePdfService invoicePdfService
    ) {
        this.mailSender = mailSender;
        this.invoicePdfService = invoicePdfService;
    }

    public EmailStatus sendBookingConfirmation(
            Booking booking,
            Invoice invoice
    ) {
        if (!mailEnabled) {
            LOGGER.info(
                    "Bokningsmejlet skickades inte eftersom "
                            + "e-postutskick är avstängt. Bokning: {}",
                    booking.getBookingNumber()
            );

            return EmailStatus.NOT_SENT;
        }

        if (!hasValidMailConfiguration(booking)) {
            return EmailStatus.NOT_SENT;
        }

        try {
            byte[] invoicePdf =
                    invoicePdfService.generateInvoicePdf(invoice);

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            StandardCharsets.UTF_8.name()
                    );

            helper.setFrom(
                    senderAddress,
                    "EriGo Travel"
            );

            helper.setTo(
                    booking.getCustomer().getEmail()
            );

            helper.setSubject(
                    "Bokningsbekräftelse "
                            + booking.getBookingNumber()
            );

            helper.setText(
                    createBookingConfirmationBody(
                            booking,
                            invoice
                    ),
                    true
            );

            helper.addAttachment(
                    "Faktura-"
                            + invoice.getInvoiceNumber()
                            + ".pdf",
                    new ByteArrayResource(invoicePdf)
            );

            mailSender.send(message);

            LOGGER.info(
                    "Bokningsbekräftelse skickades för bokning {}.",
                    booking.getBookingNumber()
            );

            return EmailStatus.SENT;
        } catch (MessagingException | MailException exception) {
            LOGGER.error(
                    "Bokningen sparades, men bokningsmejlet "
                            + "kunde inte skickas för bokning {}.",
                    booking.getBookingNumber(),
                    exception
            );

            return EmailStatus.FAILED;
        } catch (Exception exception) {
            LOGGER.error(
                    "Ett oväntat fel uppstod när bokningsmejlet "
                            + "skulle skickas för bokning {}.",
                    booking.getBookingNumber(),
                    exception
            );

            return EmailStatus.FAILED;
        }
    }

    public EmailStatus sendCancellationConfirmation(
            Booking booking
    ) {
        if (!mailEnabled) {
            LOGGER.info(
                    "Avbokningsmejlet skickades inte eftersom "
                            + "e-postutskick är avstängt. Bokning: {}",
                    booking.getBookingNumber()
            );

            return EmailStatus.NOT_SENT;
        }

        if (!hasValidMailConfiguration(booking)) {
            return EmailStatus.NOT_SENT;
        }

        try {
            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            false,
                            StandardCharsets.UTF_8.name()
                    );

            helper.setFrom(
                    senderAddress,
                    "EriGo Travel"
            );

            helper.setTo(
                    booking.getCustomer().getEmail()
            );

            helper.setSubject(
                    "Avbokningsbekräftelse "
                            + booking.getBookingNumber()
            );

            helper.setText(
                    createCancellationConfirmationBody(booking),
                    true
            );

            mailSender.send(message);

            LOGGER.info(
                    "Avbokningsbekräftelse skickades för bokning {}.",
                    booking.getBookingNumber()
            );

            return EmailStatus.SENT;
        } catch (MessagingException | MailException exception) {
            LOGGER.error(
                    "Bokningen avbokades, men avbokningsmejlet "
                            + "kunde inte skickas för bokning {}.",
                    booking.getBookingNumber(),
                    exception
            );

            return EmailStatus.FAILED;
        } catch (Exception exception) {
            LOGGER.error(
                    "Ett oväntat fel uppstod när avbokningsmejlet "
                            + "skulle skickas för bokning {}.",
                    booking.getBookingNumber(),
                    exception
            );

            return EmailStatus.FAILED;
        }
    }

    private boolean hasValidMailConfiguration(
            Booking booking
    ) {
        if (senderAddress == null
                || senderAddress.isBlank()) {

            LOGGER.warn(
                    "Mejlet skickades inte eftersom "
                            + "MAIL_USERNAME saknas."
            );

            return false;
        }

        if (booking.getCustomer().getEmail() == null
                || booking.getCustomer().getEmail().isBlank()) {

            LOGGER.warn(
                    "Mejlet skickades inte eftersom kunden saknar "
                            + "e-postadress. Bokning: {}",
                    booking.getBookingNumber()
            );

            return false;
        }

        return true;
    }

    private String createBookingConfirmationBody(
            Booking booking,
            Invoice invoice
    ) {
        return """
                <!DOCTYPE html>
                <html lang="sv">
                <body style="
                    margin: 0;
                    padding: 24px;
                    font-family: Arial, Helvetica, sans-serif;
                    color: #172326;
                    background: #f5f7f6;
                ">
                    <div style="
                        max-width: 650px;
                        margin: 0 auto;
                        padding: 32px;
                        background: #ffffff;
                        border-radius: 16px;
                    ">
                        <h1 style="
                            margin-top: 0;
                            color: #006b74;
                        ">
                            Tack för din bokning!
                        </h1>

                        <p>
                            Hej %s,
                        </p>

                        <p>
                            Din resa till <strong>%s</strong>
                            är bokad.
                        </p>

                        <div style="
                            margin: 24px 0;
                            padding: 20px;
                            background: #f5f7f6;
                            border-radius: 12px;
                        ">
                            <p>
                                <strong>Kundnummer:</strong>
                                %s
                            </p>

                            <p>
                                <strong>Bokningsnummer:</strong>
                                %s
                            </p>

                            <p>
                                <strong>Fakturanummer:</strong>
                                %s
                            </p>

                            <p>
                                <strong>Betalningsreferens:</strong>
                                %s
                            </p>
                        </div>

                        <h2>
                            Reseinformation
                        </h2>

                        <p>
                            <strong>Hotell:</strong>
                            %s
                        </p>

                        <p>
                            <strong>Avresa:</strong>
                            %s
                        </p>

                        <p>
                            <strong>Hemresa:</strong>
                            %s
                        </p>

                        <p>
                            <strong>Antal resenärer:</strong>
                            %d
                        </p>

                        <p>
                            <strong>Totalpris:</strong>
                            %s
                        </p>

                        <p>
                            Din faktura finns bifogad som PDF.
                        </p>

                        <p style="
                            margin-top: 30px;
                            color: #667477;
                        ">
                            Vänliga hälsningar<br>
                            EriGo Travel
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(
                        booking.getCustomer().getFirstName()
                ),
                escapeHtml(
                        booking.getDeparture()
                                .getTravel()
                                .getDestination()
                ),
                escapeHtml(
                        booking.getCustomer()
                                .getCustomerNumber()
                ),
                escapeHtml(
                        booking.getBookingNumber()
                ),
                escapeHtml(
                        invoice.getInvoiceNumber()
                ),
                escapeHtml(
                        invoice.getPaymentReference()
                ),
                escapeHtml(
                        booking.getDeparture()
                                .getTravel()
                                .getHotelName()
                ),
                booking.getDeparture()
                        .getDepartureDate()
                        .format(DATE_FORMATTER),
                booking.getDeparture()
                        .getReturnDate()
                        .format(DATE_FORMATTER),
                booking.getNumberOfTravelers(),
                formatMoney(booking.getTotalPrice())
        );
    }

    private String createCancellationConfirmationBody(
            Booking booking
    ) {
        String refundInformation;

        if (booking.getRefundAmount() > 0) {
            refundInformation = """
                    <p>
                        <strong>Belopp som återbetalas:</strong>
                        %s
                    </p>

                    <p>
                        Återbetalningen hanteras enligt våra
                        avbokningsvillkor.
                    </p>
                    """.formatted(
                    formatMoney(booking.getRefundAmount())
            );
        } else {
            refundInformation = """
                    <p>
                        <strong>Belopp som återbetalas:</strong>
                        0 kr
                    </p>
                    """;
        }

        return """
                <!DOCTYPE html>
                <html lang="sv">
                <body style="
                    margin: 0;
                    padding: 24px;
                    font-family: Arial, Helvetica, sans-serif;
                    color: #172326;
                    background: #f5f7f6;
                ">
                    <div style="
                        max-width: 650px;
                        margin: 0 auto;
                        padding: 32px;
                        background: #ffffff;
                        border-radius: 16px;
                    ">
                        <h1 style="
                            margin-top: 0;
                            color: #b42318;
                        ">
                            Din bokning har avbokats
                        </h1>

                        <p>
                            Hej %s,
                        </p>

                        <p>
                            Din bokning till <strong>%s</strong>
                            har avbokats.
                        </p>

                        <div style="
                            margin: 24px 0;
                            padding: 20px;
                            background: #fff7ed;
                            border-radius: 12px;
                        ">
                            <p>
                                <strong>Kundnummer:</strong>
                                %s
                            </p>

                            <p>
                                <strong>Bokningsnummer:</strong>
                                %s
                            </p>

                            <p>
                                <strong>Avbokad:</strong>
                                %s
                            </p>
                        </div>

                        <h2>
                            Ekonomisk sammanställning
                        </h2>

                        <p>
                            <strong>Inbetalt belopp:</strong>
                            %s
                        </p>

                        <p>
                            <strong>Vid avbokning förloras:</strong>
                            %s
                        </p>

                        %s

                        <p>
                            <strong>Kvar att betala:</strong>
                            0 kr
                        </p>

                        <p>
                            Du blir inte betalningsskyldig för något
                            ytterligare belopp efter avbokningen.
                        </p>

                        <h2>
                            Resan som avbokades
                        </h2>

                        <p>
                            <strong>Hotell:</strong>
                            %s
                        </p>

                        <p>
                            <strong>Planerad avresa:</strong>
                            %s
                        </p>

                        <p style="
                            margin-top: 30px;
                            color: #667477;
                        ">
                            Vänliga hälsningar<br>
                            EriGo Travel
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(
                        booking.getCustomer().getFirstName()
                ),
                escapeHtml(
                        booking.getDeparture()
                                .getTravel()
                                .getDestination()
                ),
                escapeHtml(
                        booking.getCustomer()
                                .getCustomerNumber()
                ),
                escapeHtml(
                        booking.getBookingNumber()
                ),
                booking.getCancelledAt()
                        .toLocalDate()
                        .format(DATE_FORMATTER),
                formatMoney(booking.getPaidAmount()),
                formatMoney(booking.getCancellationFee()),
                refundInformation,
                escapeHtml(
                        booking.getDeparture()
                                .getTravel()
                                .getHotelName()
                ),
                booking.getDeparture()
                        .getDepartureDate()
                        .format(DATE_FORMATTER)
        );
    }

    private String formatMoney(int amount) {
        return String.format("%,d kr", amount)
                .replace(',', ' ');
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}