package com.example.loanmanagement.enums;

/**
 * Whether a loan product is available for new applications.
 * INACTIVE loan types remain in the DB (historical applications
 * still reference them) but cannot be chosen for new applications.
 */
public enum LoanTypeStatus {
    ACTIVE,
    INACTIVE
}