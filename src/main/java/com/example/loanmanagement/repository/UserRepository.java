package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entities.
 *
 * The @EntityGraph(attributePaths = "roles") annotation tells Hibernate to
 * fetch the lazy roles collection in the SAME query via LEFT JOIN FETCH,
 * instead of issuing a second query when getRoles() is called. This matters
 * because authentication is on the hot path of every request — one query
 * per login beats two.
 *
 * Without @EntityGraph, calling user.getRoles() outside a transaction would
 * throw LazyInitializationException. With it, roles are eagerly materialized
 * by Hibernate inside the repository call itself.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}