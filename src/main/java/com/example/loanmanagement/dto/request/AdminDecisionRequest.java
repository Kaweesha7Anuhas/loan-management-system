package com.example.loanmanagement.dto.request;

import com.example.loanmanagement.enums.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDecisionRequest {

    @NotNull(message = "Decision is required (APPROVED or REJECTED)")
    private ApplicationStatus decision;

    @NotBlank(message = "Admin remarks are required")
    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String adminRemarks;
}