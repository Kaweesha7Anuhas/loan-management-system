package com.example.loanmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user attempts an operation they are not
 * permitted to perform. Maps to HTTP 403 Forbidden.
 *
 * Distinction from Spring Security's AccessDeniedException:
 *   - AccessDeniedException is thrown by Spring Security when a @PreAuthorize
 *     check fails at the METHOD level. The GlobalExceptionHandler maps it
 *     to 403 as well (see Phase 3.3).
 *   - UnauthorizedOperationException is for SERVICE-LAYER authorization
 *     checks — e.g., verifying that a loan belongs to the current customer,
 *     or a role check that can't be expressed as a @PreAuthorize annotation
 *     (because it depends on runtime state).
 *
 * Both map to 403 — the frontend handles them identically. The distinction
 * is where the check happens (annotation vs code), which matters for testing.
 *
 * Examples:
 *   - Cashier attempting to disburse a loan (should be caught by @PreAuthorize
 *     first — this is defence in depth)
 *   - Loan Officer attempting to approve their own recommendation
 *   - Non-admin attempting to view audit logs
 *   - Any user attempting to view/modify another user's resource
 */
public class UnauthorizedOperationException extends ApiException {

    public UnauthorizedOperationException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}