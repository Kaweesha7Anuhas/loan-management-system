package com.example.loanmanagement.service.impl;

import com.example.loanmanagement.dto.response.AdminDashboardResponse;
import com.example.loanmanagement.dto.response.CashierDashboardResponse;
import com.example.loanmanagement.dto.response.OfficerDashboardResponse;
import com.example.loanmanagement.enums.ApplicationStatus;
import com.example.loanmanagement.enums.LoanStatus;
import com.example.loanmanagement.repository.CustomerRepository;
import com.example.loanmanagement.repository.LoanApplicationRepository;
import com.example.loanmanagement.repository.LoanRepository;
import com.example.loanmanagement.repository.RepaymentRepository;
import com.example.loanmanagement.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final CustomerRepository customerRepository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;

    public DashboardServiceImpl(CustomerRepository customerRepository,
                                LoanApplicationRepository applicationRepository,
                                LoanRepository loanRepository,
                                RepaymentRepository repaymentRepository) {
        this.customerRepository = customerRepository;
        this.applicationRepository = applicationRepository;
        this.loanRepository = loanRepository;
        this.repaymentRepository = repaymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboardResponse getAdminDashboard() {
        return AdminDashboardResponse.builder()
                .totalCustomers(customerRepository.count())
                .totalApplications(applicationRepository.count())
                .pendingApplications(applicationRepository.countByApplicationStatus(ApplicationStatus.SUBMITTED) +
                                     applicationRepository.countByApplicationStatus(ApplicationStatus.UNDER_REVIEW) +
                                     applicationRepository.countByApplicationStatus(ApplicationStatus.RECOMMENDED))
                .approvedApplications(applicationRepository.countByApplicationStatus(ApplicationStatus.APPROVED))
                .rejectedApplications(applicationRepository.countByApplicationStatus(ApplicationStatus.REJECTED))
                .activeLoans(loanRepository.countByLoanStatus(LoanStatus.ACTIVE))
                .totalDisbursedAmount(loanRepository.sumTotalDisbursedAmount())
                .totalOutstandingBalance(loanRepository.sumTotalOutstandingBalance())
                .totalRepaymentsCollected(repaymentRepository.sumTotalRepayments())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public OfficerDashboardResponse getOfficerDashboard() {
        return OfficerDashboardResponse.builder()
                .totalApplicationsCreated(applicationRepository.count())
                .submittedApplications(applicationRepository.countByApplicationStatus(ApplicationStatus.SUBMITTED))
                .underReviewApplications(applicationRepository.countByApplicationStatus(ApplicationStatus.UNDER_REVIEW))
                .recommendedApplications(applicationRepository.countByApplicationStatus(ApplicationStatus.RECOMMENDED))
                .approvedApplications(applicationRepository.countByApplicationStatus(ApplicationStatus.APPROVED))
                .rejectedApplications(applicationRepository.countByApplicationStatus(ApplicationStatus.REJECTED))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public CashierDashboardResponse getCashierDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());

        return CashierDashboardResponse.builder()
                .repaymentsToday(repaymentRepository.sumRepaymentsOnDate(today))
                .repaymentsThisMonth(repaymentRepository.sumRepaymentsFromDate(startOfMonth))
                .activeLoans(loanRepository.countByLoanStatus(LoanStatus.ACTIVE))
                .completedLoans(loanRepository.countByLoanStatus(LoanStatus.COMPLETED))
                .build();
    }
}