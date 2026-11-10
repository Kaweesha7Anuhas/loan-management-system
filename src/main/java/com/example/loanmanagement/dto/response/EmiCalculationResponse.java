package com.example.loanmanagement.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class EmiCalculationResponse {
    private BigDecimal principalAmount;
    private BigDecimal annualInterestRate;
    private Integer tenureMonths;
    
    private BigDecimal monthlyEmi;
    private BigDecimal totalPayable;
    private BigDecimal totalInterest;
}