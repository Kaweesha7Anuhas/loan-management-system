package com.example.loanmanagement.dto.response;

import com.example.loanmanagement.enums.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full detail response for a single customer.
 * Returned by GET /api/customers/{id} and as the payload of create/update.
 *
 * Never includes the entity directly — decouples DB schema from API contract.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private Long id;
    private String customerCode;
    private String firstName;
    private String lastName;
    private String fullName;
    private String nicNumber;
    private LocalDate dateOfBirth;
    private String email;
    private String phoneNumber;
    private String address;
    private String city;
    private String occupation;
    private BigDecimal monthlyIncome;
    private CustomerStatus customerStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}