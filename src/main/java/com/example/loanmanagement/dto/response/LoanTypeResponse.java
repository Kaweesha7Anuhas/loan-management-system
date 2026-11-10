package com.example.loanmanagement.dto.response;

import com.example.loanmanagement.enums.LoanTypeStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class LoanTypeResponse {
    private Long id;
    private String loanTypeCode;
    private String loanTypeName;
    private String description;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private BigDecimal annualInterestRate;
    private Integer minimumTenureMonths;
    private Integer maximumTenureMonths;
    private LoanTypeStatus loanTypeStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}