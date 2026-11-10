package com.example.loanmanagement.dto.response;

import com.example.loanmanagement.enums.LoanStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class LoanResponse {
    private Long id;
    private String loanAccountNumber;
    private Long applicationId;
    private String applicationNumber;
    private Long customerId;
    private String customerFullName;
    private BigDecimal principalAmount;
    private BigDecimal annualInterestRate;
    private Integer tenureMonths;
    private BigDecimal totalPayable;
    private BigDecimal totalPaid;
    private BigDecimal outstandingBalance;
    private LocalDate disbursementDate;
    private LocalDate maturityDate;
    private LoanStatus loanStatus;
}