package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.Customer;
import com.example.loanmanagement.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Customer.
 *
 * The search methods return a Page<Customer> — Spring Data applies the
 * Pageable (offset + limit + sort) automatically. The @Query'ies use
 * LOWER(...) for case-insensitive matching, and an empty :search param
 * short-circuits to "no filter" without issuing LIKE clauses.
 *
 * Concurrency caveat on findMaxCustomerSequence():
 *  - This is a MAX()+1 pattern, which races under concurrent inserts
 *    (two requests can read the same max, then both insert next+1).
 *  - For an internship-educational project this is acceptable — the
 *    actual unique constraint on customer_code will refuse the second
 *    insert, and the service retries.
 *  - Production systems use a dedicated sequence table with SELECT FOR
 *    UPDATE, or a DB-native SEQUENCE object. We may revisit if needed.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerCode(String customerCode);

    Optional<Customer> findByNicNumber(String nicNumber);

    Optional<Customer> findByEmail(String email);

    boolean existsByCustomerCode(String customerCode);

    boolean existsByNicNumber(String nicNumber);

    boolean existsByEmail(String email);

    /**
     * Search WITHOUT status filter.
     * Empty :search → no filter (short-circuits on the first OR).
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE (:search = ''
                   OR LOWER(c.firstName)    LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName)     LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.nicNumber)    LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.phoneNumber)  LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.email)        LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Customer> searchByText(@Param("search") String search, Pageable pageable);

    /**
     * Search WITH status filter.
     * Same predicate as above, plus the status equality.
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE (:search = ''
                   OR LOWER(c.firstName)    LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName)     LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.nicNumber)    LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.phoneNumber)  LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.email)        LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')))
              AND c.customerStatus = :status
            """)
    Page<Customer> searchByTextAndStatus(
            @Param("search") String search,
            @Param("status") CustomerStatus status,
            Pageable pageable);

    /**
     * Return the maximum numeric sequence embedded in existing customer codes.
     * customer_code format: "CUS-000042" → SUBSTRING(..., 5) = "000042".
     *
     * Native SQL because JPQL doesn't have CAST(... AS UNSIGNED).
     * COALESCE handles the "no rows" case by returning 0.
     */
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED)), 0) FROM customers",
        nativeQuery = true)
    Long findMaxCustomerSequence();
}