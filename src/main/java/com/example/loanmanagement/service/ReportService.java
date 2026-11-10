package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.response.CollectionReportResponse;
import com.example.loanmanagement.dto.response.PortfolioReportResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    PortfolioReportResponse getPortfolioReport();
    List<CollectionReportResponse> getCollectionReport(LocalDate startDate, LocalDate endDate);
}