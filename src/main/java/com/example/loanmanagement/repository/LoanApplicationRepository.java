package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.LoanApplication;
import com.example.loanmanagement.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);
    
    boolean existsByApplicationNumber(String applicationNumber);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(application_number, '-', -1) AS UNSIGNED)), 0) FROM loan_applications", 
        nativeQuery = true)
    Long findMaxApplicationSequence();

    // We need to JOIN FETCH customer and loanType to avoid N+1 queries in the list view
    @Query(value = """
            SELECT a FROM LoanApplication a
            JOIN FETCH a.customer c
            JOIN FETCH a.loanType lt
            LEFT JOIN FETCH a.createdBy
            WHERE (:search = ''
                   OR LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
           countQuery = """
            SELECT COUNT(a) FROM LoanApplication a
            JOIN a.customer c
            WHERE (:search = ''
                   OR LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LoanApplication> searchByText(@Param("search") String search, Pageable pageable);

    @Query(value = """
            SELECT a FROM LoanApplication a
            JOIN FETCH a.customer c
            JOIN FETCH a.loanType lt
            LEFT JOIN FETCH a.createdBy
            WHERE (:search = ''
                   OR LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')))
              AND a.applicationStatus = :status
            """,
           countQuery = """
            SELECT COUNT(a) FROM LoanApplication a
            JOIN a.customer c
            WHERE (:search = ''
                   OR LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')))
              AND a.applicationStatus = :status
            """)
    Page<LoanApplication> searchByTextAndStatus(
            @Param("search") String search,
            @Param("status") ApplicationStatus status,
            Pageable pageable);

            long countByApplicationStatus(com.example.loanmanagement.enums.ApplicationStatus status);
}