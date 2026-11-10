package com.example.loanmanagement.util;

import java.time.YearMonth;

/**
 * Formatters for human-readable business identifiers.
 *
 * IMPORTANT — this class does NOT generate sequence numbers. The service
 * layer supplies the next `long` sequence value (typically `maxSequence + 1`
 * queried from the database, or a dedicated sequence table). This class
 * only FORMATS the identifier consistently:
 *
 *   customerCode(42)                        → "CUS-000042"
 *   applicationNumber(2026-11, 42)          → "APP-202611-000042"
 *   loanAccountNumber(2026, 7)              → "LN-2026-000007"
 *   receiptNumber(2026, 123)                → "RCP-2026-000123"
 *
 * Keeping formatting pure makes it trivially unit-testable (no DB needed)
 * and lets the service own the sequence-generation concern, which may
 * change (max+1 now, dedicated sequence table later) without touching this.
 */
public final class IdGenerator {

    private static final String CUSTOMER_PREFIX      = "CUS";
    private static final String APPLICATION_PREFIX   = "APP";
    private static final String LOAN_ACCOUNT_PREFIX  = "LN";
    private static final String RECEIPT_PREFIX       = "RCP";

    private IdGenerator() {
        // Utility class — do not instantiate
    }

    /** "CUS-000042" */
    public static String customerCode(long sequence) {
        return String.format("%s-%06d", CUSTOMER_PREFIX, sequence);
    }

    /** "APP-202611-000042" */
    public static String applicationNumber(YearMonth yearMonth, long sequence) {
        return String.format("%s-%04d%02d-%06d",
                APPLICATION_PREFIX,
                yearMonth.getYear(),
                yearMonth.getMonthValue(),
                sequence);
    }

    /** "LN-2026-000007" */
    public static String loanAccountNumber(int year, long sequence) {
        return String.format("%s-%04d-%06d", LOAN_ACCOUNT_PREFIX, year, sequence);
    }

    /** "RCP-2026-000123" */
    public static String receiptNumber(int year, long sequence) {
        return String.format("%s-%04d-%06d", RECEIPT_PREFIX, year, sequence);
    }
}