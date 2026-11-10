package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.LoanType;
import com.example.loanmanagement.enums.LoanTypeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanTypeRepository extends JpaRepository<LoanType, Long> {

    Optional<LoanType> findByLoanTypeCode(String loanTypeCode);

    boolean existsByLoanTypeCode(String loanTypeCode);
    boolean existsByLoanTypeName(String loanTypeName);

    @Query("""
            SELECT lt FROM LoanType lt
            WHERE (:search = ''
                   OR LOWER(lt.loanTypeCode) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(lt.loanTypeName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LoanType> searchByText(@Param("search") String search, Pageable pageable);

    @Query("""
            SELECT lt FROM LoanType lt
            WHERE (:search = ''
                   OR LOWER(lt.loanTypeCode) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(lt.loanTypeName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND lt.loanTypeStatus = :status
            """)
    Page<LoanType> searchByTextAndStatus(
            @Param("search") String search,
            @Param("status") LoanTypeStatus status,
            Pageable pageable);
}