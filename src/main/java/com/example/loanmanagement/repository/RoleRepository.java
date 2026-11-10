package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Role entities. Roles are a small, seeded table —
 * only three rows (ADMIN, LOAN_OFFICER, CASHIER). No pagination or
 * search needed.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Look up a role by its unique name, e.g. "ADMIN".
     * Used by DataInitializer to seed and by UserService to assign roles.
     */
    Optional<Role> findByName(String name);

    /** Used by DataInitializer to make seeding idempotent. */
    boolean existsByName(String name);
}