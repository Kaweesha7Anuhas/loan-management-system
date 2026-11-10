package com.example.loanmanagement.dto.response;

import com.example.loanmanagement.enums.ApplicationStatus;
import com.example.loanmanagement.enums.RecommendationDecision;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class LoanApplicationResponse {
    private Long id;
    private String applicationNumber;
    private Long customerId;
    private String customerFullName;
    private Long loanTypeId;
    private String loanTypeName;
    
    private BigDecimal requestedAmount;
    private BigDecimal annualInterestRate;
    private Integer requestedTenureMonths;
    
    private LocalDate applicationDate;
    private ApplicationStatus applicationStatus;
    private String purpose;
    private String officerRemarks;
    private String adminRemarks;
    private RecommendationDecision recommendedDecision;
    
    private String createdByUsername;
    private String reviewedByUsername;
    private String decidedByUsername;
    private LocalDateTime decisionDate;
    private LocalDateTime createdAt;
    
    // NEW FIELD
    private boolean disbursed; 
}