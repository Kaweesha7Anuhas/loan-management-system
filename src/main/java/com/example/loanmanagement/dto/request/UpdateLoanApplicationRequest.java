package com.example.loanmanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateLoanApplicationRequest {

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "1.00", message = "Requested amount must be greater than zero")
    private BigDecimal requestedAmount;

    @NotNull(message = "Requested tenure is required")
    @Min(value = 1, message = "Tenure must be at least 1 month")
    private Integer requestedTenureMonths;

    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    private String purpose;
}