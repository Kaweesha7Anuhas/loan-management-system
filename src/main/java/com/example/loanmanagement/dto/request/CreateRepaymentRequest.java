package com.example.loanmanagement.dto.request;

import com.example.loanmanagement.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateRepaymentRequest {

    @NotNull(message = "Loan ID is required")
    private Long loanId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal paymentAmount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 100, message = "Transaction reference must not exceed 100 characters")
    private String transactionReference;

    private LocalDate paymentDate;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}