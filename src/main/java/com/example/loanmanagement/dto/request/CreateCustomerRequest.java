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
 * Request body for POST /api/customers.
 *
 * Validation here is the FIRST line of defence. The database's unique
 * constraints (customer_code, nic_number, email) are the ULTIMATE arbiter —
 * service-layer checks and this DTO check can race under concurrent requests.
 *
 * All @Size / @Pattern messages are user-facing — they appear in the
 * `fieldErrors` map of the ApiError response (see GlobalExceptionHandler).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    /**
     * Sri Lankan NIC: either 9 digits + V/X (old) or 12 digits (new).
     * Examples: 200012345678 (new), 987654321V (old).
     */
    @NotBlank(message = "NIC number is required")
    @Pattern(
            regexp = "^(?:\\d{9}[VvXx]|\\d{12})$",
            message = "NIC must be either 12 digits (new) or 9 digits + V/X (old)"
    )
    private String nicNumber;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    /**
     * Sri Lankan phone: 10 digits starting with 0. Examples: 0771234567.
     */
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