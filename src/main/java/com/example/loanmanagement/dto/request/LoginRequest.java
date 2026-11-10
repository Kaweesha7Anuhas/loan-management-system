package com.example.loanmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /api/auth/login.
 *
 * `username` accepts either a username OR an email — the service-layer
 * CustomUserDetailsService tries both. The field is named `username` to
 * keep the JSON contract simple for the frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @Size(max = 100, message = "Username or email is too long")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 200, message = "Password is too long")
    private String password;
}