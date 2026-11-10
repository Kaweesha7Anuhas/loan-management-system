package com.example.loanmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all application-level exceptions that map cleanly to an
 * HTTP status. The GlobalExceptionHandler catches this once and returns
 * `getStatus()` — no per-subclass handler method is needed.
 *
 * Runtime (unchecked) exception is deliberate: Spring's @Transactional rolls
 * back only on unchecked exceptions by default. A checked exception thrown
 * mid-transaction would commit partial writes — catastrophic for financial
 * operations (a payment recorded but the loan balance not updated, etc.).
 *
 * Concrete subclasses (see siblings in this package) fix the HTTP status:
 *   ResourceNotFoundException       → 404
 *   DuplicateResourceException      → 409
 *   BusinessRuleException           → 400
 *   InvalidStateTransitionException → 409
 *   UnauthorizedOperationException  → 403
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}