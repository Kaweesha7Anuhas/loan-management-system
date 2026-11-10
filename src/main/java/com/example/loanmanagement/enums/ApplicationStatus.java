package com.example.loanmanagement.enums;

/**
 * Lifecycle states of a loan application.
 *
 * Transitions are enforced in the service layer (LoanApplicationService),
 * NOT inside this enum. Keeping enums "dumb" avoids mixing data with behaviour
 * and makes unit-testing the state machine straightforward.
 *
 * Allowed transitions:
 *   DRAFT          → SUBMITTED, CANCELLED
 *   SUBMITTED      → UNDER_REVIEW, CANCELLED
 *   UNDER_REVIEW   → RECOMMENDED, CANCELLED
 *   RECOMMENDED    → APPROVED, REJECTED
 *   APPROVED       → (terminal — becomes a Loan via disbursement)
 *   REJECTED       → (terminal)
 *   CANCELLED      → (terminal)
 */
public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    RECOMMENDED,
    APPROVED,
    REJECTED,
    CANCELLED
}