package com.example.loanmanagement.entity;

import com.example.loanmanagement.enums.LoanTypeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A loan product offered by the finance company (Personal, Business, Vehicle,
 * Education ...).
 *
 * This entity is managed by Admin only (Phase 6). When a loan application is
 * submitted, the applicable values from this row — amount range, interest rate,
 * tenure range — are SNAPSHOTTED onto the LoanApplication. Editing a LoanType
 * afterwards must never alter existing applications or disbursed loans.
 *
 * Design notes:
 *  - loanTypeCode is a stable business identifier ("PL", "BL", "VL", "EL").
 *    Unlike loanTypeName, codes are not meant to change.
 *  - annualInterestRate is stored as DECIMAL(5,2) — 12.50 represents 12.50%.
 *  - LoanTypeStatus controls whether new applications may reference this product.
 *    INACTIVE rows remain in the DB because historical applications reference them.
 */
@Entity
@Table(
    name = "loan_types",
    indexes = {
        @Index(name = "idx_loan_types_code", columnList = "loan_type_code"),
        @Index(name = "idx_loan_types_name", columnList = "loan_type_name")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable short code, e.g. "PL", "BL", "VL", "EL". Unique.
     */
    @Column(name = "loan_type_code", nullable = false, unique = true, length = 20)
    private String loanTypeCode;

    @Column(name = "loan_type_name", nullable = false, unique = true, length = 100)
    private String loanTypeName;

    @Column(name = "description", length = 500)
    private String description;

    /**
     * Range of allowed loan amounts for this product. Both in LKR.
     */
    @Column(name = "minimum_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minimumAmount;

    @Column(name = "maximum_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal maximumAmount;

    /**
     * Annual interest rate as a percentage. 12.50 = 12.50% per annum.
     * DECIMAL(5,2) — 5 digits total, 2 decimal places (max 999.99).
     */
    @Column(name = "annual_interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualInterestRate;

    @Column(name = "minimum_tenure_months", nullable = false)
    private Integer minimumTenureMonths;

    @Column(name = "maximum_tenure_months", nullable = false)
    private Integer maximumTenureMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type_status", nullable = false, length = 20)
    @Builder.Default
    private LoanTypeStatus loanTypeStatus = LoanTypeStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}