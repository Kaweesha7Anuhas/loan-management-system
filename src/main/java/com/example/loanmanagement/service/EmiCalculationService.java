package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.response.EmiCalculationResponse;
import java.math.BigDecimal;

public interface EmiCalculationService {
    EmiCalculationResponse calculateEmi(BigDecimal principal, BigDecimal annualRate, Integer tenureMonths);
}