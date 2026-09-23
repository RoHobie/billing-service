package com.rohobie.billing.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rohobie.billing.domain.BillLineItem;
import com.rohobie.billing.domain.BillingRun;
import com.rohobie.billing.exception.ResourceNotFoundException;
import com.rohobie.billing.repository.BillLineItemRepository;
import com.rohobie.billing.repository.BillingRunRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * InvoicePdfService
 *
 * Generates formal, auditable PDF billing statements for completed month-end runs.
 * Produces structured corporate documents containing executive summaries, itemised trip entries,
 * rate computation traces, and formal reconciliation totals in Rupees.
 *
 * Assumption: Standard A4 page orientation accommodates itemised trip records.
 * Design decision: Monetary values rendered via scale-2 BigDecimal formatting to maintain strict precision.
 */
@Slf4j
@Service
public class InvoicePdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 41, 59));
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(100, 116, 139));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(15, 23, 42));
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(30, 41, 59));
    private static final Font BODY_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(15, 23, 42));
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(148, 163, 184));

    private final BillingRunRepository billingRunRepository;
    private final BillLineItemRepository billLineItemRepository;

    public InvoicePdfService(BillingRunRepository billingRunRepository,
                             BillLineItemRepository billLineItemRepository) {
        this.billingRunRepository = billingRunRepository;
        this.billLineItemRepository = billLineItemRepository;
    }

    /**
     * Generates a downloadable binary PDF invoice for the specified billing run.
     *
     * @param billingRunId the unique identifier of the billing run
     * @return byte array containing the compiled PDF document
     */
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long billingRunId) {
        log.info("Generating corporate PDF invoice for billing run ID: {}", billingRunId);

        BillingRun run = billingRunRepository.findById(billingRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Billing run with id " + billingRunId + " not found"));

        List<BillLineItem> lineItems = billLineItemRepository.findByBillingRunId(billingRunId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header Banner
            Paragraph companyTitle = new Paragraph("FastFleet Rentals", TITLE_FONT);
            companyTitle.setAlignment(Element.ALIGN_LEFT);
            document.add(companyTitle);

            Paragraph docType = new Paragraph("COMMERCIAL INVOICE & RECONCILIATION STATEMENT", HEADER_FONT);
            docType.setSpacingAfter(4f);
            document.add(docType);

            Paragraph subtitle = new Paragraph("FastFleet Corporate Fleet Services | GSTIN: 29AAAAA0000A1Z5 | fleet-billing@fastfleet.com", SUBTITLE_FONT);
            subtitle.setSpacingAfter(15f);
            document.add(subtitle);

            // 2. Metadata Grid (Invoice Details & Fleet Asset Details)
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(15f);

            PdfPCell cellLeft = new PdfPCell();
            cellLeft.setBorder(0);
            cellLeft.addElement(new Paragraph("INVOICE METADATA", BODY_BOLD));
            cellLeft.addElement(new Paragraph("Invoice Number: INV-2026-" + String.format("%04d", run.getId()), BODY_FONT));
            cellLeft.addElement(new Paragraph("Billing Month: " + run.getBillingMonth(), BODY_FONT));
            cellLeft.addElement(new Paragraph("Generated On: " + run.getRunAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")), BODY_FONT));
            cellLeft.addElement(new Paragraph("Status: " + run.getStatus().name(), BODY_FONT));
            metaTable.addCell(cellLeft);

            PdfPCell cellRight = new PdfPCell();
            cellRight.setBorder(0);
            cellRight.addElement(new Paragraph("FLEET & VENDOR DETAILS", BODY_BOLD));
            cellRight.addElement(new Paragraph("Vehicle Registration: " + run.getVehicle().getRegistrationNumber(), BODY_FONT));
            cellRight.addElement(new Paragraph("Vehicle Type: " + run.getVehicle().getType(), BODY_FONT));
            cellRight.addElement(new Paragraph("Vendor: " + run.getVehicle().getVendor().getName(), BODY_FONT));
            cellRight.addElement(new Paragraph("Vendor Contact: " + run.getVehicle().getVendor().getContactEmail(), BODY_FONT));
            metaTable.addCell(cellRight);

            document.add(metaTable);

            // 3. Financial Summary Card
            long totalBasePaisa = lineItems.stream().mapToLong(BillLineItem::getBasePaisa).sum();
            long totalExtrasPaisa = lineItems.stream().mapToLong(BillLineItem::getExtraChargesPaisa).sum();
            long totalFixedPaisa = lineItems.stream().mapToLong(BillLineItem::getFixedFeeSharePaisa).sum();
            long grandTotalPaisa = lineItems.stream().mapToLong(BillLineItem::getTotalPaisa).sum();

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20f);

            addSummaryHeaderCell(summaryTable, "Base Distance Fare");
            addSummaryHeaderCell(summaryTable, "Surcharges & Tolls");
            addSummaryHeaderCell(summaryTable, "Fixed Contract Share");
            addSummaryHeaderCell(summaryTable, "Grand Total (INR)");

            addSummaryDataCell(summaryTable, formatPaisaToRupees(totalBasePaisa));
            addSummaryDataCell(summaryTable, formatPaisaToRupees(totalExtrasPaisa));
            addSummaryDataCell(summaryTable, formatPaisaToRupees(totalFixedPaisa));
            addSummaryDataCell(summaryTable, formatPaisaToRupees(grandTotalPaisa));

            document.add(summaryTable);

            // 4. Itemised Per-Trip Breakdown Table
            Paragraph itemisedHeader = new Paragraph("ITEMISED TRIP BREAKDOWN (" + lineItems.size() + " TRIPS)", HEADER_FONT);
            itemisedHeader.setSpacingAfter(8f);
            document.add(itemisedHeader);

            PdfPTable itemTable = new PdfPTable(new float[]{1.0f, 1.2f, 1.2f, 1.2f, 1.4f, 4.0f});
            itemTable.setWidthPercentage(100);
            itemTable.setSpacingAfter(20f);

            addTableHeaderCell(itemTable, "Trip ID");
            addTableHeaderCell(itemTable, "Distance");
            addTableHeaderCell(itemTable, "Base (₹)");
            addTableHeaderCell(itemTable, "Extras (₹)");
            addTableHeaderCell(itemTable, "Total (₹)");
            addTableHeaderCell(itemTable, "Audit Computation Note");

            boolean alternate = false;
            for (BillLineItem item : lineItems) {
                Color rowBg = alternate ? new Color(248, 250, 252) : Color.WHITE;
                alternate = !alternate;

                addTableRowCell(itemTable, String.valueOf(item.getTrip().getId()), rowBg);
                addTableRowCell(itemTable, item.getTrip().getDistanceKm() + " km", rowBg);
                addTableRowCell(itemTable, formatPaisaToRupees(item.getBasePaisa()), rowBg);
                addTableRowCell(itemTable, formatPaisaToRupees(item.getExtraChargesPaisa()), rowBg);
                addTableRowCell(itemTable, formatPaisaToRupees(item.getTotalPaisa()), rowBg);
                addTableRowCell(itemTable, item.getComputationNote() != null ? item.getComputationNote() : "-", rowBg);
            }

            document.add(itemTable);

            // 5. Payment Terms & Sign-off
            Paragraph notes = new Paragraph(
                    "Payment Terms: Net 30 days from billing date. All rates calculated per active contract schedule. "
                            + "For inquiries or rate disputes, please contact accounts-payable@fastfleet.com quoting the Invoice Number above.",
                    FOOTER_FONT
            );
            notes.setSpacingBefore(10f);
            document.add(notes);

            document.close();
        } catch (Exception ex) {
            log.error("Failed to compile PDF document for billing run ID: {}", billingRunId, ex);
            throw new RuntimeException("Error generating invoice PDF: " + ex.getMessage(), ex);
        }

        return out.toByteArray();
    }

    private void addTableHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(new Color(30, 41, 59));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addTableRowCell(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY_FONT));
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }

    private void addSummaryHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY_BOLD));
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setPadding(8f);
        cell.setBorderColor(new Color(203, 213, 225));
        table.addCell(cell);
    }

    private void addSummaryDataCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(Color.WHITE);
        cell.setPadding(8f);
        cell.setBorderColor(new Color(203, 213, 225));
        table.addCell(cell);
    }

    /**
     * Converts integer paisa to Rupee string (₹ XX.XX) using BigDecimal to avoid double precision issues.
     */
    private String formatPaisaToRupees(long paisa) {
        BigDecimal rupees = BigDecimal.valueOf(paisa).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return "₹ " + rupees.toPlainString();
    }
}
