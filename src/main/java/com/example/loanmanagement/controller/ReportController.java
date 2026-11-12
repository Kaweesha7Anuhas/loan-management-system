package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.response.ApiResponse;
import com.example.loanmanagement.dto.response.CollectionReportResponse;
import com.example.loanmanagement.dto.response.PortfolioReportResponse;
import com.example.loanmanagement.service.ReportService;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/portfolio")
    public ResponseEntity<ApiResponse<PortfolioReportResponse>> getPortfolioReport() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getPortfolioReport()));
    }

    @GetMapping("/collections")
    public ResponseEntity<ApiResponse<List<CollectionReportResponse>>> getCollectionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getCollectionReport(startDate, endDate)));
    }

    // =========================================================================
    // NEW PDF EXPORT ENDPOINT
    // =========================================================================
    @GetMapping("/export-pdf")
    public void exportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // 1. Create the Document
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // 2. Add Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Paragraph title = new Paragraph("Loan Management System", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(71, 85, 105));
            Paragraph subTitle = new Paragraph("Financial Portfolio Report", subTitleFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subTitle);

            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
            Paragraph date = new Paragraph("Period: " + startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) +
                    " to " + endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), dateFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);

            // 3. Fetch Real Data from Service
            PortfolioReportResponse portfolio = reportService.getPortfolioReport();
            List<CollectionReportResponse> collections = reportService.getCollectionReport(startDate, endDate);

            // 4. Portfolio Summary Table
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingBefore(10);
            summaryTable.setSpacingAfter(25);

            addSummaryCell(summaryTable, "Total Disbursed Principal", portfolio.getTotalDisbursedPrincipal(), true);
            addSummaryCell(summaryTable, "Total Interest Expected", portfolio.getTotalInterestExpected(), false);
            addSummaryCell(summaryTable, "Total Outstanding Balance", portfolio.getTotalOutstanding(), true);

            document.add(summaryTable);

            // 5. Daily Collection Summary Table
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Paragraph sectionTitle = new Paragraph("Daily Collection Summary", sectionFont);
            sectionTitle.setSpacingAfter(10);
            document.add(sectionTitle);

            PdfPTable collectionTable = new PdfPTable(3);
            collectionTable.setWidthPercentage(100);
            collectionTable.setWidths(new float[]{2f, 1.5f, 1.5f});

            // Table Headers
            addTableHeader(collectionTable, "Date");
            addTableHeader(collectionTable, "Transactions");
            addTableHeader(collectionTable, "Total Collected");

            // Table Rows (Iterate over real data)
            for (CollectionReportResponse row : collections) {
                addTableRow(collectionTable, 
                        row.getDate().toString(), 
                        row.getTransactionCount() + " payments", 
                        formatCurrency(row.getTotalCollected()));
            }

            document.add(collectionTable);
            document.close();

            // 6. Send to Browser
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"LMS_Financial_Report_" + LocalDate.now() + ".pdf\"");
            response.getOutputStream().write(out.toByteArray());
            response.getOutputStream().flush();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating PDF");
            } catch (Exception ignored) {}
        }
    }

    // --- PDF Helper Methods ---

    private void addSummaryCell(PdfPTable table, String label, Number value, boolean isGrayBg) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(71, 85, 105));
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        PdfPCell valueCell = new PdfPCell(new Phrase(formatCurrency(value), valueFont));

        labelCell.setPadding(8);
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        if (isGrayBg) {
            labelCell.setBackgroundColor(new Color(248, 250, 252));
            valueCell.setBackgroundColor(new Color(248, 250, 252));
        }

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String headerTitle) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        PdfPCell header = new PdfPCell(new Phrase(headerTitle, headerFont));
        header.setBackgroundColor(new Color(37, 99, 235)); // Blue
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setPadding(6);
        table.addCell(header);
    }

    private void addTableRow(PdfPTable table, String date, String transactions, String collected) {
        Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        PdfPCell dateCell = new PdfPCell(new Phrase(date, rowFont));
        PdfPCell txCell = new PdfPCell(new Phrase(transactions, rowFont));
        PdfPCell amtCell = new PdfPCell(new Phrase(collected, rowFont));

        dateCell.setPadding(5);
        txCell.setPadding(5);
        amtCell.setPadding(5);

        txCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(dateCell);
        table.addCell(txCell);
        table.addCell(amtCell);
    }

    private String formatCurrency(Number amount) {
        if (amount == null) return "LKR 0.00";
        return "LKR " + String.format("%,.2f", amount.doubleValue());
    }
}