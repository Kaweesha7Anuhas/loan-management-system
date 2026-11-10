package com.example.loanmanagement.dto.response;

import com.example.loanmanagement.enums.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight customer DTO for list/pagination views.
 *
 * Rationale: a customer list page shows code, name, NIC, phone, and status.
 * Returning the full CustomerResponse for 100 rows pulls address, income,
 * occupation, timestamps — none of which the list needs. Smaller payloads
 * mean faster JSON serialization and less bandwidth. Classic DTO projection.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSummaryResponse {

    private Long id;
    private String customerCode;
    private String fullName;
    private String nicNumber;
    private String phoneNumber;
    private String email;
    private CustomerStatus customerStatus;
}