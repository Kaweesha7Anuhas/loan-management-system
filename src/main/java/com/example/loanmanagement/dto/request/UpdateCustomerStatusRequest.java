package com.example.loanmanagement.dto.request;

import com.example.loanmanagement.enums.CustomerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for PATCH /api/customers/{id}/status.
 *
 * Separate from UpdateCustomerRequest for two reasons:
 *  1. Status changes are a distinct operation (may require different
 *     authorization, may be audited separately).
 *  2. PATCH with only the changed field is the correct HTTP semantics —
 *     not sending the entire customer just to flip a flag.
 *
 * The enum value in the JSON body must be one of ACTIVE or INACTIVE
 * (case-sensitive by default). Jackson will reject anything else with
 * a 400 Bad Request via the HttpMessageNotReadableException handler.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerStatusRequest {

    @NotNull(message = "Status is required")
    private CustomerStatus status;
}