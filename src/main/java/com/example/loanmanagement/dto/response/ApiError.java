package com.example.loanmanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response returned by GlobalExceptionHandler for every
 * non-2xx outcome.
 *
 * Shape:
 *   {
 *     "status": 400,
 *     "error": "Bad Request",
 *     "message": "Validation failed",
 *     "path": "/api/customers",
 *     "timestamp": "2026-11-10T...",
 *     "fieldErrors": {
 *       "email": "must be a well-formed email address",
 *       "nicNumber": "must not be blank"
 *     }
 *   }
 *
 * `fieldErrors` is populated only for MethodArgumentNotValidException
 * (Jakarta Bean Validation failures). For all other errors it is null
 * and omitted by @JsonInclude.
 *
 * IMPORTANT: We NEVER put stack traces or SQL strings in `message`.
 * The handler maps internal exceptions to safe, user-facing strings.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private int status;
    private String error;
    private String message;
    private String path;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Field-level validation errors (nullable). */
    private Map<String, String> fieldErrors;
}