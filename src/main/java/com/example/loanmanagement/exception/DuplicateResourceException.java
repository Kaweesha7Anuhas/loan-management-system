package com.example.loanmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation would create a resource that violates a
 * uniqueness constraint. Maps to HTTP 409 Conflict.
 *
 * This should be thrown PROACTIVELY in the service layer (check first,
 * then insert) to give the user a clean message. The database's unique
 * constraint remains the ultimate arbiter — if the pre-check races, the
 * DataIntegrityViolationException is caught by the handler and translated
 * into a duplicate-resource response anyway.
 *
 * Examples:
 *   throw new DuplicateResourceException("Customer", "nicNumber", "200012345678");
 *   throw DuplicateResourceException.of("User", "username", "jdoe");
 */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public static DuplicateResourceException of(String resourceName, String field, Object value) {
        return new DuplicateResourceException(
                String.format("%s already exists with %s = %s", resourceName, field, value)
        );
    }
}