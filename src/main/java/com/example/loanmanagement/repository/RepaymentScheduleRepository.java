package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {
    List<RepaymentSchedule> findByLoanIdOrderByInstalmentNumberAsc(Long loanId);
    boolean existsByLoanId(Long loanId);
}