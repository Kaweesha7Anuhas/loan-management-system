package com.example.loanmanagement.mapper;

import com.example.loanmanagement.dto.response.LoanResponse;
import com.example.loanmanagement.dto.response.RepaymentScheduleResponse;
import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.entity.RepaymentSchedule;
import org.springframework.stereotype.Component;

@Component
public class LoanMapper {

    public LoanResponse toResponse(Loan entity) {
        if (entity == null) return null;

        return LoanResponse.builder()
                .id(entity.getId())
                .loanAccountNumber(entity.getLoanAccountNumber())
                .applicationId(entity.getLoanApplication().getId())
                .applicationNumber(entity.getLoanApplication().getApplicationNumber())
                .customerId(entity.getCustomer().getId())
                .customerFullName(entity.getCustomer().getFullName())
                .principalAmount(entity.getPrincipalAmount())
                .annualInterestRate(entity.getAnnualInterestRate())
                .tenureMonths(entity.getTenureMonths())
                .totalPayable(entity.getTotalPayable())
                .totalPaid(entity.getTotalPaid())
                .outstandingBalance(entity.getOutstandingBalance())
                .disbursementDate(entity.getDisbursementDate())
                .maturityDate(entity.getMaturityDate())
                .loanStatus(entity.getLoanStatus())
                .build();
    }

    public RepaymentScheduleResponse toScheduleResponse(RepaymentSchedule entity) {
        if (entity == null) return null;

        return RepaymentScheduleResponse.builder()
                .id(entity.getId())
                .instalmentNumber(entity.getInstalmentNumber())
                .dueDate(entity.getDueDate())
                .openingBalance(entity.getOpeningBalance())
                .principalAmount(entity.getPrincipalAmount())
                .interestAmount(entity.getInterestAmount())
                .instalmentAmount(entity.getInstalmentAmount())
                .paidAmount(entity.getPaidAmount())
                .remainingAmount(entity.getRemainingAmount())
                .paymentStatus(entity.getPaymentStatus())
                .build();
    }
}