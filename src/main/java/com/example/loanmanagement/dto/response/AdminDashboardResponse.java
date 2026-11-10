package com.example.loanmanagement.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class AdminDashboardResponse {
    private long totalCustomers;
    private long totalApplications;
    private long pendingApplications;
    private long approvedApplications;
    private long rejectedApplications;
    private long activeLoans;
    private BigDecimal totalDisbursedAmount;
    private BigDecimal totalOutstandingBalance;
    private BigDecimal totalRepaymentsCollected;
}