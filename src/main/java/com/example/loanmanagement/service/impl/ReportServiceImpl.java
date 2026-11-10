package com.example.loanmanagement.service.impl;

import com.example.loanmanagement.dto.response.CollectionReportResponse;
import com.example.loanmanagement.dto.response.PortfolioReportResponse;
import com.example.loanmanagement.enums.LoanStatus;
import com.example.loanmanagement.repository.LoanRepository;
import com.example.loanmanagement.repository.ReportRepository;
import com.example.loanmanagement.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final LoanRepository loanRepository;

    public ReportServiceImpl(ReportRepository reportRepository, LoanRepository loanRepository) {
        this.reportRepository = reportRepository;
        this.loanRepository = loanRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PortfolioReportResponse getPortfolioReport() {
        BigDecimal disbursed = safe(loanRepository.sumTotalDisbursedAmount());
        BigDecimal interest = safe(reportRepository.sumTotalExpectedInterest());
        BigDecimal payable = safe(reportRepository.sumTotalExpectedPayable());
        BigDecimal collected = safe(loanRepository.sumTotalRepaymentsCollected());
        BigDecimal outstanding = safe(loanRepository.sumTotalOutstandingBalance());

        return PortfolioReportResponse.builder()
                .totalDisbursedPrincipal(disbursed)
                .totalInterestExpected(interest)
                .totalPayableAmount(payable)
                .totalCollected(collected)
                .totalOutstanding(outstanding)
                .activeLoansCount(loanRepository.countByLoanStatus(LoanStatus.ACTIVE))
                .completedLoansCount(loanRepository.countByLoanStatus(LoanStatus.COMPLETED))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public List<CollectionReportResponse> getCollectionReport(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = reportRepository.getCollectionSummary(startDate, endDate);
        
        return results.stream().map(row -> CollectionReportResponse.builder()
                .date((LocalDate) row[0])
                .transactionCount((Long) row[1])
                .totalCollected((BigDecimal) row[2])
                .build()).toList();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}