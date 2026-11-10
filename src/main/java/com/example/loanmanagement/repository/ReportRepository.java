package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Repayment, Long> {

    @Query("SELECT SUM(l.totalPayable - l.principalAmount) FROM Loan l")
    BigDecimal sumTotalExpectedInterest();

    @Query("SELECT SUM(l.totalPayable) FROM Loan l")
    BigDecimal sumTotalExpectedPayable();

    @Query("""
            SELECT r.paymentDate, COUNT(r), SUM(r.paymentAmount)
            FROM Repayment r
            WHERE r.paymentDate BETWEEN :startDate AND :endDate
            GROUP BY r.paymentDate
            ORDER BY r.paymentDate DESC
            """)
    List<Object[]> getCollectionSummary(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}