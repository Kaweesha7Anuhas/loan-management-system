package com.example.loanmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a security role in the system.
 * 
 * Roles are seeded at startup (see DataInitializer in Phase 4) with three
 * fixed values: ADMIN, LOAN_OFFICER, CASHIER. 
 *
 * The name is stored as a String rather than an enum so that adding a new
 * role later does not require a schema migration. The RoleName enum provides
 * compile-time safety wherever we reference a role by name in code.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique role name, e.g. "ADMIN", "LOAN_OFFICER", "CASHIER".
     * Length 50 is plenty for any future role name.
     */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}