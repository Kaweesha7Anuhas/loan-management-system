package com.example.loanmanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateLoanTypeRequest {

    @NotBlank(message = "Loan type code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    private String loanTypeCode;

    @NotBlank(message = "Loan type name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String loanTypeName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Minimum amount is required")
    @DecimalMin(value = "0.00", message = "Minimum amount cannot be negative")
    private BigDecimal minimumAmount;

    @NotNull(message = "Maximum amount is required")
    @DecimalMin(value = "0.01", message = "Maximum amount must be greater than zero")
    private BigDecimal maximumAmount;

    @NotNull(message = "Annual interest rate is required")
    @DecimalMin(value = "0.00", message = "Interest rate cannot be negative")
    private BigDecimal annualInterestRate;

    @NotNull(message = "Minimum tenure is required")
    @Min(value = 1, message = "Minimum tenure must be at least 1 month")
    private Integer minimumTenureMonths;

    @NotNull(message = "Maximum tenure is required")
    @Min(value = 1, message = "Maximum tenure must be at least 1 month")
    private Integer maximumTenureMonths;
}