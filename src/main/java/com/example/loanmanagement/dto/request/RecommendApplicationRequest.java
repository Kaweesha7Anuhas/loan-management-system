package com.example.loanmanagement.dto.request;

import com.example.loanmanagement.enums.RecommendationDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendApplicationRequest {

    @NotNull(message = "Decision is required (RECOMMEND_APPROVE or RECOMMEND_REJECT)")
    private RecommendationDecision decision;

    @NotBlank(message = "Officer remarks are required when recommending")
    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String officerRemarks;
}