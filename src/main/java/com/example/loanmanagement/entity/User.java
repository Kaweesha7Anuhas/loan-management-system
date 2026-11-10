package com.example.loanmanagement.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A system user (Admin, Loan Officer, or Cashier).
 * 
 * Authentication: Spring Security loads the user by username, verifies the
 * BCrypt hash of the submitted password, and maps the roles to authorities
 * (ROLE_ADMIN, ROLE_LOAN_OFFICER, ROLE_CASHIER).
 * 
 * The roles relationship is ManyToMany and LAZY: a user rarely needs roles
 * except at login time or when displaying the sidebar. Loading eagerly would
 * add a join to every query that touches a User.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_email",    columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Login identifier. Unique across the system.
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * BCrypt-hashed password. NEVER store plain text.
     * BCrypt hashes are 60 chars; 100 leaves room for algorithm upgrades.
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * Enables Admin to deactivate an account without deleting it
     * (preserves audit history).
     * @Builder.Default is REQUIRED — without it, Lombok's @Builder
     * would set this field to null instead of true.
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Join table: user_roles(user_id, role_id).
     * Cascade PERSIST/MERGE only — never REMOVE, because deleting a user
     * should not delete the roles that other users share.
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

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

    /**
     * Convenience method for adding a role without worrying about the
     * HashSet being null (defensive coding against deserialized/refreshed
     * entities).
     */
    public void addRole(Role role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(role);
    }
}