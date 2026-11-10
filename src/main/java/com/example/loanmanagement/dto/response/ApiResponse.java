package com.example.loanmanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Standard success response wrapper for ALL API endpoints.
 *
 * Every successful controller response — a single customer, a page of
 * loans, a dashboard summary — uses this envelope:
 *
 *   {
 *     "success": true,
 *     "message": "Customer created",
 *     "data": { ... },           // the actual payload
 *     "timestamp": "2026-11-10T..."
 *   }
 *
 * The frontend's AJAX layer parses the envelope once, then works with `data`.
 * Errors use ApiError (separate class) — kept distinct because error shape
 * should not need a generic `data` field.
 *
 * @JsonInclude(NON_NULL) omits null fields — smaller payloads and cleaner JSON.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Simple success with payload. */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /** Success with a custom message (e.g. "Customer created"). */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /** Success with no payload (DELETE operations, void actions). */
    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .build();
    }
}