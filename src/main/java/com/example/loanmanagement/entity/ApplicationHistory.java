package com.example.loanmanagement.entity;

import com.example.loanmanagement.enums.ApplicationStatus;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Append-only audit trail of a LoanApplication's state transitions.
 *
 * One row per transition. Rows are NEVER updated or deleted — a "correction"
 * is a new row, preserving the full history. This satisfies:
 *  - Regulatory audit requirements (who changed what, when, why)
 *  - Debugging production incidents (how did this application get here?)
 *  - Separation of duties verification (officer recommended ≠ admin decided)
 *
 * Deliberately excluded from this entity:
 *  - @PreUpdate / @PreUpdate lifecycle hooks (this is append-only)
 *  - Any @ManyToOne(cascade = REMOVE) toward LoanApplication — deleting an
 *    application must not cascade-delete history (history outlives the record)
 */
@Entity
@Table(
    name = "application_history",
    indexes = {
        @Index(name = "idx_hist_application", columnList = "loan_application_id"),
        @Index(name = "idx_hist_changed_at",  columnList = "changed_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_application_id", nullable = false, updatable = false)
    private LoanApplication loanApplication;

    /**
     * Status before the transition.
     * NULL only on the very first row (initial DRAFT creation).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private ApplicationStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false, updatable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    /**
     * Free-text note attached to this specific transition.
     * For a recommendation row: officer's remarks.
     * For a final decision row: admin's remarks.
     */
    @Column(name = "remarks", length = 1000)
    private String remarks;

    @PrePersist
    protected void onCreate() {
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
    }
}