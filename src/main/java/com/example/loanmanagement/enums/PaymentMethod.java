package com.example.loanmanagement.enums;

/**
 * How a customer paid a repayment.
 * No real payment gateway is integrated — these are record-keeping
 * categories only.
 */
public enum PaymentMethod {
    CASH,
    BANK_TRANSFER,
    CARD
}