package com.example.loanmanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for PUT /api/customers/{id}.
 *
 * Design choice: `nicNumber` and `customerCode` are NOT editable.
 *  - customerCode is a permanent business identifier (like an account number).
 *  - nicNumber identifies the person legally. Changing it would silently
 *    reassign identity — dangerous for financial records.
 * If a NIC must be corrected, that is an Admin-only special operation
 * (with audit log) rather than an ordinary PUT field.
 *
 * `customerStatus` is also NOT in this DTO — status changes go through
 * a dedicated PATCH /api/customers/{id}/status endpoint, so the two
 * concerns (edit details vs. toggle active) don't collide.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^0\\d{9}$",
            message = "Phone number must be 10 digits starting with 0"
    )
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "Occupation must not exceed 100 characters")
    private String occupation;

    @DecimalMin(value = "0.00", message = "Monthly income cannot be negative")
    private BigDecimal monthlyIncome;
}