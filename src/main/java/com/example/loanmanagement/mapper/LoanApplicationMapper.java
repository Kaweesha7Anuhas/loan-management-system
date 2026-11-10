package com.example.loanmanagement.mapper;

import com.example.loanmanagement.dto.response.LoanApplicationResponse;
import com.example.loanmanagement.entity.LoanApplication;
import org.springframework.stereotype.Component;

@Component
public class LoanApplicationMapper {

    // ADD the boolean parameter
    public LoanApplicationResponse toResponse(LoanApplication entity, boolean disbursed) {
        if (entity == null) return null;

        return LoanApplicationResponse.builder()
                .id(entity.getId())
                .applicationNumber(entity.getApplicationNumber())
                .customerId(entity.getCustomer().getId())
                .customerFullName(entity.getCustomer().getFullName())
                .loanTypeId(entity.getLoanType().getId())
                .loanTypeName(entity.getLoanType().getLoanTypeName())
                .requestedAmount(entity.getRequestedAmount())
                .annualInterestRate(entity.getAnnualInterestRate())
                .requestedTenureMonths(entity.getRequestedTenureMonths())
                .applicationDate(entity.getApplicationDate())
                .applicationStatus(entity.getApplicationStatus())
                .purpose(entity.getPurpose())
                .officerRemarks(entity.getOfficerRemarks())
                .adminRemarks(entity.getAdminRemarks())
                .recommendedDecision(entity.getRecommendedDecision())
                .createdByUsername(entity.getCreatedBy() != null ? entity.getCreatedBy().getUsername() : null)
                .reviewedByUsername(entity.getReviewedBy() != null ? entity.getReviewedBy().getUsername() : null)
                .decidedByUsername(entity.getDecidedBy() != null ? entity.getDecidedBy().getUsername() : null)
                .decisionDate(entity.getDecisionDate())
                .createdAt(entity.getCreatedAt())
                .disbursed(disbursed) // NEW
                .build();
    }
}