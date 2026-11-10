package com.example.loanmanagement.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Centralized BigDecimal utilities for ALL financial calculations.
 *
 * Why a dedicated class?
 *  - Enforces a single rounding policy (HALF_UP, scale 2).
 *  - Prevents the "new BigDecimal(\"0.01\") sprinkled everywhere" problem
 *    where one file rounds at scale 2 and another at scale 4.
 *  - Makes the calculation engine (Phase 9) readable: `monetary(x)` says
 *    "this is a currency value" at a glance.
 *
 * Rounding policy: HALF_UP at scale 2.
 *  This is the standard for currency in Sri Lanka and most jurisdictions
 *  where this project is taught. Some banks use HALF_EVEN (banker's rounding)
 *  — if you ever port this code, that's the line to change.
 *
 * NEVER use `double` anywhere in a financial formula. `0.1 + 0.2 != 0.3`
 * for double — for LKR millions, the errors compound visibly.
 */
public final class BigDecimalUtil {

    /** Scale used for all stored/displayed currency amounts. */
    public static final int MONETARY_SCALE = 2;

    /** Scale used for stored percentages (e.g. 12.50). */
    public static final int RATE_SCALE = 4;

    /**
     * Scale used for intermediate calculation steps (e.g. monthly rate
     * derived from annual). High enough to avoid compound rounding errors
     * before the final monetary rounding.
     */
    public static final int CALCULATION_SCALE = 10;

    /** Rounding mode used everywhere. HALF_UP is the standard for LKR. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private BigDecimalUtil() {
        // Utility class — do not instantiate
    }

    // ─────────────────────────────────────────────
    // Rounding helpers
    // ─────────────────────────────────────────────

    /** Round to 2 decimals using the project's rounding policy. Null-safe. */
    public static BigDecimal monetary(BigDecimal value) {
        return value == null ? null : value.setScale(MONETARY_SCALE, ROUNDING);
    }

    /** Round to 2 decimals, treating null as zero. */
    public static BigDecimal monetaryZ(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONETARY_SCALE, ROUNDING);
    }

    /**
     * Convert an annual PERCENTAGE (e.g. 12.50) into a MONTHLY DECIMAL
     * (e.g. 0.0104166667). Used by the EMI formula.
     */
    public static BigDecimal monthlyRate(BigDecimal annualPercent) {
        if (annualPercent == null || annualPercent.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return annualPercent
                .divide(BigDecimal.valueOf(100), CALCULATION_SCALE, ROUNDING)
                .divide(BigDecimal.valueOf(12), CALCULATION_SCALE, ROUNDING);
    }

    /**
     * Compute (1 + r)^n with high precision.
     * The exponent is an int (number of months) — matches BigDecimal.pow's
     * contract. MathContext.DECIMAL128 gives 34 significant digits, plenty
     * for even the longest loan.
     */
    public static BigDecimal pow(BigDecimal base, int exponent) {
        return base.pow(exponent, MathContext.DECIMAL128);
    }

    // ─────────────────────────────────────────────
    // Sign checks
    // ─────────────────────────────────────────────

    public static boolean isZero(BigDecimal value) {
        return value == null || value.signum() == 0;
    }

    public static boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    // ─────────────────────────────────────────────
    // Null-safe arithmetic (monetary rounding applied)
    // ─────────────────────────────────────────────

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return safe(a).add(safe(b)).setScale(MONETARY_SCALE, ROUNDING);
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return safe(a).subtract(safe(b)).setScale(MONETARY_SCALE, ROUNDING);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return safe(a).multiply(safe(b)).setScale(MONETARY_SCALE, ROUNDING);
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (b == null || b.signum() == 0) {
            throw new ArithmeticException("Division by zero in financial calculation");
        }
        return safe(a).divide(b, MONETARY_SCALE, ROUNDING);
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}