package com.example.loanmanagement.mapper;

import com.example.loanmanagement.dto.response.RepaymentResponse;
import com.example.loanmanagement.entity.Repayment;
import org.springframework.stereotype.Component;

@Component
public class RepaymentMapper {

    public RepaymentResponse toResponse(Repayment entity) {
        if (entity == null) return null;

        return RepaymentResponse.builder()
                .id(entity.getId())
                .receiptNumber(entity.getReceiptNumber())
                .loanId(entity.getLoan().getId())
                .loanAccountNumber(entity.getLoan().getLoanAccountNumber())
                .customerFullName(entity.getCustomer().getFullName())
                .paymentDate(entity.getPaymentDate())
                .paymentAmount(entity.getPaymentAmount())
                .paymentMethod(entity.getPaymentMethod())
                .transactionReference(entity.getTransactionReference())
                .receivedBy(entity.getReceivedBy().getUsername())
                .remarks(entity.getRemarks())
                .newOutstandingBalance(entity.getLoan().getOutstandingBalance())
                .loanStatus(entity.getLoan().getLoanStatus().name())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}