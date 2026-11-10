package com.example.loanmanagement.dto.response;

import com.example.loanmanagement.enums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class RepaymentResponse {
    private Long id;
    private String receiptNumber;
    private Long loanId;
    private String loanAccountNumber;
    private String customerFullName;
    private LocalDate paymentDate;
    private BigDecimal paymentAmount;
    private PaymentMethod paymentMethod;
    private String transactionReference;
    private String receivedBy;
    private String remarks;
    
    private BigDecimal newOutstandingBalance;
    private String loanStatus;
    private LocalDateTime createdAt;
}