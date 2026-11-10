package com.example.loanmanagement.enums;

/**
 * Whether a customer is currently allowed to hold or apply for loans.
 * Deactivated customers remain in the DB for history but cannot be
 * selected on new loan applications.
 */
public enum CustomerStatus {
    ACTIVE,
    INACTIVE
}