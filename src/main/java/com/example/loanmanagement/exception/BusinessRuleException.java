package com.example.loanmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the request is syntactically valid but violates a business rule.
 * Maps to HTTP 400 Bad Request.
 *
 * Contrast with InvalidStateTransitionException (409): that one is for state
 * machine violations. This one is for numeric/threshold rules.
 *
 * Examples:
 *   - "Requested amount 25,000 is below the minimum 50,000 for Personal Loan"
 *   - "Tenure of 6 months is below the minimum 12 months"
 *   - "Repayment amount 100,000 exceeds outstanding balance 45,000"
 *   - "Loan type is inactive and cannot accept new applications"
 *   - "Approved amount must be greater than zero"
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}