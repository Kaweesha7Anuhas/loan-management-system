package com.example.loanmanagement.service.impl;

import com.example.loanmanagement.dto.response.EmiCalculationResponse;
import com.example.loanmanagement.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EmiCalculationServiceImplTest {

    private EmiCalculationServiceImpl emiCalculationService;

    @BeforeEach
    void setUp() {
        emiCalculationService = new EmiCalculationServiceImpl();
    }

    @Test
    @DisplayName("Should calculate correct EMI for standard loan (12.5% interest)")
    void testStandardLoanCalculation() {
        // Arrange
        BigDecimal principal = new BigDecimal("500000.00");
        BigDecimal annualRate = new BigDecimal("12.50");
        Integer tenure = 24;

        // Act
        EmiCalculationResponse result = emiCalculationService.calculateEmi(principal, annualRate, tenure);

        // Assert
        assertNotNull(result);
        // Correct calculated EMI
        assertEquals(new BigDecimal("23653.65"), result.getMonthlyEmi());
        assertEquals(new BigDecimal("567687.60"), result.getTotalPayable());
        assertEquals(new BigDecimal("67687.60"), result.getTotalInterest());
    }

    @Test
    @DisplayName("Should calculate correctly when interest rate is zero")
    void testZeroInterestLoan() {
        // Arrange
        BigDecimal principal = new BigDecimal("120000.00");
        BigDecimal annualRate = BigDecimal.ZERO;
        Integer tenure = 12;

        // Act
        EmiCalculationResponse result = emiCalculationService.calculateEmi(principal, annualRate, tenure);

        // Assert
        // 120,000 / 12 = 10,000 exactly
        assertEquals(new BigDecimal("10000.00"), result.getMonthlyEmi());
        assertEquals(new BigDecimal("120000.00"), result.getTotalPayable());
        // Total interest should be 0.00 with scale 2
        assertEquals(new BigDecimal("0.00"), result.getTotalInterest());
    }

    @Test
    @DisplayName("Should throw exception for negative principal")
    void testNegativePrincipal() {
        BigDecimal principal = new BigDecimal("-1000.00");
        BigDecimal annualRate = new BigDecimal("10.00");
        Integer tenure = 12;

        assertThrows(BusinessRuleException.class, () -> 
                emiCalculationService.calculateEmi(principal, annualRate, tenure));
    }

    @Test
    @DisplayName("Should throw exception for zero tenure")
    void testZeroTenure() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal annualRate = new BigDecimal("10.00");
        Integer tenure = 0;

        assertThrows(BusinessRuleException.class, () -> 
                emiCalculationService.calculateEmi(principal, annualRate, tenure));
    }
}