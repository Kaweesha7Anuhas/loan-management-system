package com.example.loanmanagement.dto.response;

import com.example.loanmanagement.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class RepaymentScheduleResponse {
    private Long id;
    private Integer instalmentNumber;
    private LocalDate dueDate;
    private BigDecimal openingBalance;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal instalmentAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private PaymentStatus paymentStatus;
}