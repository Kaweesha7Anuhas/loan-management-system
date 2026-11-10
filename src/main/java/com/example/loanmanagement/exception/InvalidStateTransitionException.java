package com.example.loanmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a status transition violates the workflow state machine.
 * Maps to HTTP 409 Conflict.
 *
 * Why 409 not 400?
 *   The request itself is well-formed, but it conflicts with the CURRENT
 *   STATE of the resource on the server. That is the textbook definition
 *   of 409. It also gives the frontend a distinct signal: "reload the
 *   resource — its state has changed since you loaded it."
 *
 * Examples:
 *   - SUBMITTED → APPROVED (must go through UNDER_REVIEW → RECOMMENDED first)
 *   - REJECTED → APPROVED (a rejected application is terminal)
 *   - Disbursing an application that is not APPROVED
 *   - Recommending an application that is already APPROVED
 *   - Recording a repayment against a COMPLETED loan
 */
public class InvalidStateTransitionException extends ApiException {

    public InvalidStateTransitionException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    /**
     * Convenience factory for the common "from → to not allowed" shape.
     * Usage: InvalidStateTransitionException.between("LoanApplication",
     *              "REJECTED", "APPROVED")
     */
    public static InvalidStateTransitionException between(
            String entityType, Object fromState, Object toState) {
        return new InvalidStateTransitionException(
                String.format("Cannot transition %s from %s to %s",
                        entityType, fromState, toState)
        );
    }
}