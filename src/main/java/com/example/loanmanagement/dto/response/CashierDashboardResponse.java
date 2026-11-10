package com.example.loanmanagement.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CashierDashboardResponse {
    private BigDecimal repaymentsToday;
    private BigDecimal repaymentsThisMonth;
    private long activeLoans;
    private long completedLoans;
}