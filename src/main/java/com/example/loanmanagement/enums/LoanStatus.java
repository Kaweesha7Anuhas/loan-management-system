package com.example.loanmanagement.enums;

/**
 * Lifecycle states of a disbursed Loan account.
 *
 * ACTIVE     – disbursed and currently being repaid
 * COMPLETED  – all instalments fully paid (outstanding balance = 0)
 * OVERDUE    – at least one instalment is past due and unpaid
 * CANCELLED  – administrative cancellation (rare, requires audit entry)
 */
public enum LoanStatus {
    ACTIVE,
    COMPLETED,
    OVERDUE,
    CANCELLED
}