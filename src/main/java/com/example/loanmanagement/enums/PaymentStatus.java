package com.example.loanmanagement.enums;

/**
 * Status of a single instalment in a repayment schedule.
 *
 * PENDING         – due in the future, nothing paid yet
 * PARTIALLY_PAID  – some amount received, but < instalmentAmount
 * PAID            – instalment fully settled
 * OVERDUE         – due date has passed and balance > 0
 *
 * OVERDUE is derived — computed at query time based on dueDate vs today.
 * A scheduled job (or on-read calculation) flips PENDING → OVERDUE.
 */
public enum PaymentStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    OVERDUE
}