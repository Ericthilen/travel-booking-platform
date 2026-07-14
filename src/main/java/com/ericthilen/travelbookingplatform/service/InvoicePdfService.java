package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.Invoice;
import com.ericthilen.travelbookingplatform.model.PaymentPlan;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] generateInvoicePdf(Invoice invoice) {
        try (
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {
            Document document = new Document();

            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.open();

            addInvoiceContent(
                    document,
                    invoice
            );

            document.close();

            return outputStream.toByteArray();
        } catch (DocumentException exception) {
            throw new IllegalStateException(
                    "Fakturan kunde inte skapas.",
                    exception
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Ett oväntat fel uppstod när fakturan skapades.",
                    exception
            );
        }
    }

    private void addInvoiceContent(
            Document document,
            Invoice invoice
    ) throws DocumentException {

        Booking booking = invoice.getBooking();

        Font titleFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                22
        );

        Font headingFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                13
        );

        Font normalFont = FontFactory.getFont(
                FontFactory.HELVETICA,
                10
        );

        Paragraph companyName = new Paragraph(
                "EriGo Travel",
                titleFont
        );

        companyName.setAlignment(
                Element.ALIGN_LEFT
        );

        document.add(companyName);

        document.add(
                new Paragraph(
                        "Faktura och bokningsbekräftelse",
                        headingFont
                )
        );

        document.add(new Paragraph(" "));

        addReferenceInformation(
                document,
                invoice,
                booking,
                headingFont,
                normalFont
        );

        document.add(new Paragraph(" "));

        addCustomerInformation(
                document,
                booking,
                headingFont,
                normalFont
        );

        document.add(new Paragraph(" "));

        addTravelInformation(
                document,
                booking,
                headingFont,
                normalFont
        );

        document.add(new Paragraph(" "));

        addPaymentInformation(
                document,
                invoice,
                booking,
                headingFont,
                normalFont
        );

        document.add(new Paragraph(" "));

        document.add(
                new Paragraph(
                        "Ange betalningsreferensen vid betalning: "
                                + invoice.getPaymentReference(),
                        headingFont
                )
        );

        document.add(new Paragraph(" "));

        document.add(
                new Paragraph(
                        "Fakturan visar bokningens aktuella "
                                + "betalningsinformation.",
                        normalFont
                )
        );

        document.add(
                new Paragraph(
                        "Denna faktura skapades automatiskt "
                                + "av EriGo Travel.",
                        normalFont
                )
        );
    }

    private void addReferenceInformation(
            Document document,
            Invoice invoice,
            Booking booking,
            Font headingFont,
            Font normalFont
    ) throws DocumentException {

        PdfPTable referenceTable =
                new PdfPTable(2);

        referenceTable.setWidthPercentage(100);

        addCell(
                referenceTable,
                "Fakturanummer",
                invoice.getInvoiceNumber(),
                headingFont,
                normalFont
        );

        addCell(
                referenceTable,
                "Fakturadatum",
                invoice.getCreatedAt()
                        .toLocalDate()
                        .format(DATE_FORMATTER),
                headingFont,
                normalFont
        );

        addCell(
                referenceTable,
                "Kundnummer",
                booking.getCustomer()
                        .getCustomerNumber(),
                headingFont,
                normalFont
        );

        addCell(
                referenceTable,
                "Bokningsnummer",
                booking.getBookingNumber(),
                headingFont,
                normalFont
        );

        addCell(
                referenceTable,
                "Betalningsreferens",
                invoice.getPaymentReference(),
                headingFont,
                normalFont
        );

        addCell(
                referenceTable,
                "Bokningsstatus",
                booking.getStatus()
                        .getDisplayName(),
                headingFont,
                normalFont
        );

        document.add(referenceTable);
    }

    private void addCustomerInformation(
            Document document,
            Booking booking,
            Font headingFont,
            Font normalFont
    ) throws DocumentException {

        document.add(
                new Paragraph(
                        "Kunduppgifter",
                        headingFont
                )
        );

        document.add(
                new Paragraph(
                        booking.getCustomer().getFirstName()
                                + " "
                                + booking.getCustomer().getLastName(),
                        normalFont
                )
        );

        document.add(
                new Paragraph(
                        booking.getCustomer().getEmail(),
                        normalFont
                )
        );

        document.add(
                new Paragraph(
                        booking.getCustomer().getPhone(),
                        normalFont
                )
        );
    }

    private void addTravelInformation(
            Document document,
            Booking booking,
            Font headingFont,
            Font normalFont
    ) throws DocumentException {

        document.add(
                new Paragraph(
                        "Reseinformation",
                        headingFont
                )
        );

        PdfPTable travelTable =
                new PdfPTable(2);

        travelTable.setWidthPercentage(100);

        addCell(
                travelTable,
                "Destination",
                booking.getDeparture()
                        .getTravel()
                        .getDestination(),
                headingFont,
                normalFont
        );

        addCell(
                travelTable,
                "Hotell",
                booking.getDeparture()
                        .getTravel()
                        .getHotelName(),
                headingFont,
                normalFont
        );

        addCell(
                travelTable,
                "Avresa",
                booking.getDeparture()
                        .getDepartureDate()
                        .format(DATE_FORMATTER),
                headingFont,
                normalFont
        );

        addCell(
                travelTable,
                "Hemresa",
                booking.getDeparture()
                        .getReturnDate()
                        .format(DATE_FORMATTER),
                headingFont,
                normalFont
        );

        addCell(
                travelTable,
                "Antal resenärer",
                String.valueOf(
                        booking.getNumberOfTravelers()
                ),
                headingFont,
                normalFont
        );

        addCell(
                travelTable,
                "Antal rum",
                String.valueOf(
                        booking.getNumberOfRooms()
                ),
                headingFont,
                normalFont
        );

        addCell(
                travelTable,
                "Rumstyp",
                booking.getRoomType().getName(),
                headingFont,
                normalFont
        );

        addCell(
                travelTable,
                "Flygnummer",
                booking.getDeparture()
                        .getOutboundFlightNumber(),
                headingFont,
                normalFont
        );

        document.add(travelTable);
    }

    private void addPaymentInformation(
            Document document,
            Invoice invoice,
            Booking booking,
            Font headingFont,
            Font normalFont
    ) throws DocumentException {

        document.add(
                new Paragraph(
                        "Betalningsinformation",
                        headingFont
                )
        );

        PdfPTable paymentTable =
                new PdfPTable(2);

        paymentTable.setWidthPercentage(100);

        addCell(
                paymentTable,
                "Totalpris",
                formatMoney(
                        booking.getTotalPrice()
                ),
                headingFont,
                normalFont
        );

        addCell(
                paymentTable,
                "Betalningsstatus",
                booking.getPaymentStatus()
                        .getDisplayName(),
                headingFont,
                normalFont
        );

        addCell(
                paymentTable,
                "Betalt hittills",
                formatMoney(
                        booking.getPaidAmount()
                ),
                headingFont,
                normalFont
        );

        addCell(
                paymentTable,
                "Återstår att betala",
                formatMoney(
                        Math.max(
                                0,
                                booking.getRemainingAmount()
                        )
                ),
                headingFont,
                normalFont
        );

        addPaymentPlanRows(
                paymentTable,
                invoice,
                booking,
                headingFont,
                normalFont
        );

        document.add(paymentTable);
    }

    private void addPaymentPlanRows(
            PdfPTable table,
            Invoice invoice,
            Booking booking,
            Font headingFont,
            Font normalFont
    ) {
        if (booking.getStatus().name()
                .equals("CANCELLED")) {

            addCell(
                    table,
                    "Avbokningskostnad",
                    formatMoney(
                            booking.getCancellationFee()
                    ),
                    headingFont,
                    normalFont
            );

            addCell(
                    table,
                    "Återbetalning",
                    formatMoney(
                            booking.getRefundAmount()
                    ),
                    headingFont,
                    normalFont
            );

            return;
        }

        if (booking.getPaymentPlan()
                == PaymentPlan.FULL_PAYMENT) {

            addCell(
                    table,
                    "Betalningstyp",
                    "Hela resan betalas direkt",
                    headingFont,
                    normalFont
            );

            addCell(
                    table,
                    "Förfallodatum",
                    invoice.getFinalPaymentDueDate()
                            .format(DATE_FORMATTER),
                    headingFont,
                    normalFont
            );

            return;
        }

        addCell(
                table,
                "Anmälningsavgift",
                formatMoney(
                        invoice.getDepositAmount()
                ),
                headingFont,
                normalFont
        );

        if (invoice.getDepositDueDate() != null) {
            addCell(
                    table,
                    "Anmälningsavgift senast",
                    invoice.getDepositDueDate()
                            .format(DATE_FORMATTER),
                    headingFont,
                    normalFont
            );
        }

        addCell(
                table,
                "Slutbetalning senast",
                invoice.getFinalPaymentDueDate()
                        .format(DATE_FORMATTER),
                headingFont,
                normalFont
        );
    }

    private void addCell(
            PdfPTable table,
            String label,
            String value,
            Font headingFont,
            Font normalFont
    ) {
        PdfPCell labelCell = new PdfPCell(
                new Phrase(
                        label,
                        headingFont
                )
        );

        labelCell.setPadding(8);

        PdfPCell valueCell = new PdfPCell(
                new Phrase(
                        value,
                        normalFont
                )
        );

        valueCell.setPadding(8);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String formatMoney(int amount) {
        return String.format(
                "%,d kr",
                amount
        ).replace(',', ' ');
    }
}