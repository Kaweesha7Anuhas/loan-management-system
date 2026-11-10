package com.example.loanmanagement.dto.request;

import com.example.loanmanagement.enums.LoanTypeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLoanTypeStatusRequest {
    @NotNull(message = "Status is required")
    private LoanTypeStatus status;
}