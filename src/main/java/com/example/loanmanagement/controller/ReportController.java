package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.response.ApiResponse;
import com.example.loanmanagement.dto.response.CollectionReportResponse;
import com.example.loanmanagement.dto.response.PortfolioReportResponse;
import com.example.loanmanagement.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
}