package com.example.loanmanagement.enums;

/**
 * Direction of a Loan Officer's recommendation on an application.
 *
 * The RECOMMENDED status alone is direction-agnostic — this enum records
 * whether the officer recommended approval or rejection. Admin uses this
 * as advisory input for the final decision.
 */
public enum RecommendationDecision {
    RECOMMEND_APPROVE,
    RECOMMEND_REJECT
}