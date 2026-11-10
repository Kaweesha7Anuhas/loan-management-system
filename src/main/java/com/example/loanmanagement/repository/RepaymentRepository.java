package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Long> {
    
    List<Repayment> findByLoanIdOrderByPaymentDateDesc(Long loanId);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(receipt_number, '-', -1) AS UNSIGNED)), 0) FROM repayments", 
        nativeQuery = true)
    Long findMaxReceiptSequence();

    @Query("SELECT COALESCE(SUM(r.paymentAmount), 0) FROM Repayment r")
    java.math.BigDecimal sumTotalRepayments();

    @Query("SELECT COALESCE(SUM(r.paymentAmount), 0) FROM Repayment r WHERE r.paymentDate = :date")
    java.math.BigDecimal sumRepaymentsOnDate(@Param("date") java.time.LocalDate date);

    @Query("SELECT COALESCE(SUM(r.paymentAmount), 0) FROM Repayment r WHERE r.paymentDate >= :startDate")
    java.math.BigDecimal sumRepaymentsFromDate(@Param("startDate") java.time.LocalDate startDate);
}