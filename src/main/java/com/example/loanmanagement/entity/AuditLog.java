package com.example.loanmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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
 * Append-only audit trail for security-relevant events.
 *
 * Rows are written by AuditLogService at key checkpoints:
 *  - LOGIN / LOGIN_FAILED / LOGOUT
 *  - CUSTOMER_CREATED / CUSTOMER_UPDATED / CUSTOMER_STATUS_CHANGED
 *  - LOAN_APPLICATION_CREATED / SUBMITTED / RECOMMENDED
 *  - LOAN_APPLICATION_APPROVED / REJECTED
 *  - LOAN_DISBURSED
 *  - REPAYMENT_RECORDED
 *  - USER_CREATED / USER_STATUS_CHANGED / ROLE_ASSIGNED
 *  - LOAN_TYPE_CREATED / UPDATED / STATUS_CHANGED
 *
 * NEVER log: passwords, session IDs, authentication tokens, or raw
 * sensitive customer data (NIC full value, card numbers, etc.).
 *
 * Rows are NEVER updated or deleted — corrections are new rows.
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_user",        columnList = "user_id"),
        @Index(name = "idx_audit_action",      columnList = "action"),
        @Index(name = "idx_audit_entity_type", columnList = "entity_type"),
        @Index(name = "idx_audit_entity_id",   columnList = "entity_id"),
        @Index(name = "idx_audit_created_at",  columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who performed the action. NULL only for system-initiated
     * events (e.g. scheduled job flipping statuses to OVERDUE).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Action code, e.g. "LOGIN", "LOAN_APPROVED", "REPAYMENT_RECORDED".
     * Stored as a String, not an enum, so new action types don't require
     * schema migrations. Values are constants in AuditAction (utility class).
     */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /**
     * Entity type this action targets, e.g. "Customer", "LoanApplication".
     * NULL for non-entity events (LOGIN, LOGOUT).
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /**
     * Primary key of the affected entity. Intentionally NOT a foreign key —
     * the log must survive if the entity is deleted. Just a numeric reference.
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * Human-readable description. Contains no passwords or sensitive data.
     * Example: "Approved loan application APP-202601-000042 for LKR 500,000"
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * JSON snapshot of the entity BEFORE the change (nullable — only for
     * update-type actions).
     */
    @Lob
    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;

    /**
     * JSON snapshot of the entity AFTER the change (nullable — only for
     * update/insert-type actions).
     */
    @Lob
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * Client IP address. Nullable — tests and system-initiated actions
     * won't have one.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Client User-Agent string (truncated). Optional; useful for security
     * investigations.
     */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}