package com.example.loanmanagement.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class PortfolioReportResponse {
    private BigDecimal totalDisbursedPrincipal;
    private BigDecimal totalInterestExpected;
    private BigDecimal totalPayableAmount;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    private long activeLoansCount;
    private long completedLoansCount;
}