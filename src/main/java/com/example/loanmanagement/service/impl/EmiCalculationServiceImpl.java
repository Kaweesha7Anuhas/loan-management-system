package com.example.loanmanagement.service.impl;

import com.example.loanmanagement.dto.response.EmiCalculationResponse;
import com.example.loanmanagement.exception.BusinessRuleException;
import com.example.loanmanagement.service.EmiCalculationService;
import com.example.loanmanagement.util.BigDecimalUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Service
public class EmiCalculationServiceImpl implements EmiCalculationService {

    @Override
    public EmiCalculationResponse calculateEmi(BigDecimal principal, BigDecimal annualRate, Integer tenureMonths) {
        
        // 1. Validate inputs
        if (principal == null || principal.signum() <= 0) {
            throw new BusinessRuleException("Principal amount must be greater than zero");
        }
        if (tenureMonths == null || tenureMonths <= 0) {
            throw new BusinessRuleException("Tenure must be at least 1 month");
        }
        if (annualRate == null || annualRate.signum() < 0) {
            throw new BusinessRuleException("Interest rate cannot be negative");
        }

        BigDecimal monthlyEmi;
        
        // 2. Handle Zero Interest Case (Simple division)
        if (annualRate.signum() == 0) {
            monthlyEmi = principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        } 
        // 3. Handle Normal Interest Case (Reducing Balance EMI Formula)
        else {
            // r = Annual Rate / 12 / 100
            BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
            
            // (1 + r)
            BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
            
            // (1 + r)^n
            BigDecimal onePlusRPowN = onePlusR.pow(tenureMonths, MathContext.DECIMAL128);
            
            // Numerator: P * r * (1 + r)^n
            BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRPowN);
            
            // Denominator: ((1 + r)^n) - 1
            BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE);
            
            // EMI = Numerator / Denominator
            monthlyEmi = numerator.divide(denominator, 2, RoundingMode.HALF_UP);
        }

        // 4. Calculate Totals
        BigDecimal totalPayable = monthlyEmi.multiply(BigDecimal.valueOf(tenureMonths)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalPayable.subtract(principal).setScale(2, RoundingMode.HALF_UP);

        return EmiCalculationResponse.builder()
                .principalAmount(principal)
                .annualInterestRate(annualRate)
                .tenureMonths(tenureMonths)
                .monthlyEmi(monthlyEmi)
                .totalPayable(totalPayable)
                .totalInterest(totalInterest)
                .build();
    }
}