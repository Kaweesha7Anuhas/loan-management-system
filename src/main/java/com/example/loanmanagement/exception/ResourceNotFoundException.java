package com.example.loanmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request references an entity that does not exist.
 * Maps to HTTP 404 Not Found.
 *
 * Examples:
 *   throw new ResourceNotFoundException("Customer", "id", 42L);
 *   throw new ResourceNotFoundException("Loan application not found");
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Convenience factory producing a standard "X with Y = Z not found" message.
     * Usage: ResourceNotFoundException.of("Customer", "id", 42L)
     * Result message: "Customer not found with id = 42"
     */
    public static ResourceNotFoundException of(String resourceName, String field, Object value) {
        return new ResourceNotFoundException(
                String.format("%s not found with %s = %s", resourceName, field, value)
        );
    }
}