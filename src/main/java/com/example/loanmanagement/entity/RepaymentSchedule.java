package com.example.loanmanagement.entity;

import com.example.loanmanagement.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row = one monthly instalment of a loan's repayment plan.
 *
 * The full schedule is generated at disbursement time (one row per month
 * across the tenure). The final instalment adjusts for rounding so that
 * after all instalments are PAID, openingBalance - sum(principalAmount)
 * equals exactly zero.
 *
 * UNIQUE(loan_id, instalment_number) prevents duplicate schedules.
 */
@Entity
@Table(
    name = "repayment_schedules",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_schedule_loan_instalment",
            columnNames = {"loan_id", "instalment_number"}
        )
    },
    indexes = {
        @Index(name = "idx_schedule_loan",  columnList = "loan_id"),
        @Index(name = "idx_schedule_due",   columnList = "due_date"),
        @Index(name = "idx_schedule_status", columnList = "payment_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    /**
     * 1-based instalment number within the schedule (1, 2, ..., tenureMonths).
     */
    @Column(name = "instalment_number", nullable = false)
    private Integer instalmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    // ─────────────────────────────────────────────
    // Money breakdown (per instalment)
    // ─────────────────────────────────────────────

    @Column(name = "opening_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestAmount;

    /**
     * Total for this instalment = principalAmount + interestAmount.
     * Stored separately for convenience (and to prevent recomputation drift).
     */
    @Column(name = "instalment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal instalmentAmount;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /**
     * Amount still owed on this specific instalment.
     * Recalculated transactionally when a repayment is allocated.
     */
    @Column(name = "remaining_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

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