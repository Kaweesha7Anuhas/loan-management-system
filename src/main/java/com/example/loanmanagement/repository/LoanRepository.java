package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanAccountNumber(String loanAccountNumber);

    boolean existsByLoanApplicationId(Long applicationId);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(loan_account_number, '-', -1) AS UNSIGNED)), 0) FROM loans", 
           nativeQuery = true)
    Long findMaxLoanSequence();

    long countByLoanStatus(LoanStatus status);

    @Query("SELECT SUM(l.principalAmount) FROM Loan l WHERE l.loanStatus != 'CANCELLED'")
    BigDecimal sumTotalDisbursedAmount();

    @Query("SELECT SUM(l.outstandingBalance) FROM Loan l WHERE l.loanStatus IN ('ACTIVE', 'OVERDUE')")
    BigDecimal sumTotalOutstandingBalance();

    @Query("SELECT SUM(l.totalPaid) FROM Loan l")
    BigDecimal sumTotalRepaymentsCollected();

    @Query(value = """
            SELECT l FROM Loan l
            JOIN FETCH l.customer c
            JOIN FETCH l.loanApplication a
            WHERE (:search = ''
                   OR LOWER(l.loanAccountNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
           countQuery = """
            SELECT COUNT(l) FROM Loan l
            JOIN l.customer c
            WHERE (:search = ''
                   OR LOWER(l.loanAccountNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Loan> searchByText(@Param("search") String search, Pageable pageable);

    @Query(value = """
            SELECT l FROM Loan l
            JOIN FETCH l.customer c
            JOIN FETCH l.loanApplication a
            WHERE (:search = ''
                   OR LOWER(l.loanAccountNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')))
              AND l.loanStatus = :status
            """,
           countQuery = """
            SELECT COUNT(l) FROM Loan l
            JOIN l.customer c
            WHERE (:search = ''
                   OR LOWER(l.loanAccountNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')))
              AND l.loanStatus = :status
            """)
    Page<Loan> searchByTextAndStatus(
            @Param("search") String search,
            @Param("status") LoanStatus status,
            Pageable pageable);
}