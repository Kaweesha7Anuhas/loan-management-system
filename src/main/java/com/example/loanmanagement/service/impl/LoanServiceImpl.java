package com.example.loanmanagement.service.impl;

import com.example.loanmanagement.dto.response.LoanResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.dto.response.RepaymentScheduleResponse;
import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.entity.LoanApplication;
import com.example.loanmanagement.entity.RepaymentSchedule;
import com.example.loanmanagement.enums.ApplicationStatus;
import com.example.loanmanagement.enums.LoanStatus;
import com.example.loanmanagement.enums.PaymentStatus;
import com.example.loanmanagement.exception.BusinessRuleException;
import com.example.loanmanagement.exception.DuplicateResourceException;
import com.example.loanmanagement.exception.ResourceNotFoundException;
import com.example.loanmanagement.mapper.LoanMapper;
import com.example.loanmanagement.repository.LoanApplicationRepository;
import com.example.loanmanagement.repository.LoanRepository;
import com.example.loanmanagement.repository.RepaymentScheduleRepository;
import com.example.loanmanagement.service.EmiCalculationService;
import com.example.loanmanagement.service.LoanService;
import com.example.loanmanagement.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanServiceImpl implements LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanServiceImpl.class);

    private final LoanRepository loanRepository;
    private final LoanApplicationRepository applicationRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final EmiCalculationService emiCalculationService;
    private final LoanMapper loanMapper;

    public LoanServiceImpl(LoanRepository loanRepository,
                           LoanApplicationRepository applicationRepository,
                           RepaymentScheduleRepository scheduleRepository,
                           EmiCalculationService emiCalculationService,
                           LoanMapper loanMapper) {
        this.loanRepository = loanRepository;
        this.applicationRepository = applicationRepository;
        this.scheduleRepository = scheduleRepository;
        this.emiCalculationService = emiCalculationService;
        this.loanMapper = loanMapper;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')") // ONLY Admin can disburse
    public LoanResponse disburse(Long applicationId) {
        LoanApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("LoanApplication", "id", applicationId));

        if (app.getApplicationStatus() != ApplicationStatus.APPROVED) {
            throw new BusinessRuleException("Only APPROVED applications can be disbursed");
        }

        if (loanRepository.existsByLoanApplicationId(applicationId)) {
            throw new DuplicateResourceException("This application has already been disbursed");
        }

        // 1. Generate Loan Account Number
        long nextSeq = loanRepository.findMaxLoanSequence() + 1;
        String loanAcctNo = IdGenerator.loanAccountNumber(LocalDate.now().getYear(), nextSeq);

        // 2. Calculate EMI for schedule generation
        var emiResult = emiCalculationService.calculateEmi(
                app.getRequestedAmount(), app.getAnnualInterestRate(), app.getRequestedTenureMonths());

        // 3. Create the Loan
        Loan loan = Loan.builder()
                .loanAccountNumber(loanAcctNo)
                .loanApplication(app)
                .customer(app.getCustomer())
                .principalAmount(app.getRequestedAmount())
                .annualInterestRate(app.getAnnualInterestRate())
                .tenureMonths(app.getRequestedTenureMonths())
                .totalPayable(emiResult.getTotalPayable())
                .disbursementDate(LocalDate.now())
                .maturityDate(LocalDate.now().plusMonths(app.getRequestedTenureMonths()))
                .totalPaid(BigDecimal.ZERO)
                .outstandingBalance(emiResult.getTotalPayable())
                .loanStatus(LoanStatus.ACTIVE)
                .build();

        Loan savedLoan = loanRepository.save(loan);

        // 4. Generate Repayment Schedule
        generateSchedule(savedLoan, emiResult.getMonthlyEmi());

        log.info("Loan disbursed: {} for application {}", savedLoan.getLoanAccountNumber(), app.getApplicationNumber());
        return loanMapper.toResponse(savedLoan);
    }

    private void generateSchedule(Loan loan, BigDecimal monthlyEmi) {
        BigDecimal monthlyRate = loan.getAnnualInterestRate().divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal openingBalance = loan.getPrincipalAmount();
        List<RepaymentSchedule> schedules = new ArrayList<>();

        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            BigDecimal interest = openingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principal;
            BigDecimal instalmentAmount;

            // Last instalment logic: clear out the remaining balance to handle rounding drift
            if (i == loan.getTenureMonths()) {
                principal = openingBalance;
                instalmentAmount = principal.add(interest);
            } else {
                principal = monthlyEmi.subtract(interest);
                instalmentAmount = monthlyEmi;
            }

            BigDecimal closingBalance = openingBalance.subtract(principal);

            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .loan(loan)
                    .instalmentNumber(i)
                    .dueDate(loan.getDisbursementDate().plusMonths(i))
                    .openingBalance(openingBalance)
                    .principalAmount(principal)
                    .interestAmount(interest)
                    .instalmentAmount(instalmentAmount)
                    .paidAmount(BigDecimal.ZERO)
                    .remainingAmount(instalmentAmount)
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();

            schedules.add(schedule);
            openingBalance = closingBalance;
        }

        scheduleRepository.saveAll(schedules);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','CASHIER')")
    public LoanResponse findById(Long id) {
        return loanRepository.findById(id)
                .map(loanMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Loan", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','CASHIER')")
    public List<RepaymentScheduleResponse> getSchedule(Long loanId) {
        if (!loanRepository.existsById(loanId)) {
            throw ResourceNotFoundException.of("Loan", "id", loanId);
        }
        return scheduleRepository.findByLoanIdOrderByInstalmentNumberAsc(loanId)
                .stream()
                .map(loanMapper::toScheduleResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','CASHIER')")
    public PageResponse<LoanResponse> findAll(String search, LoanStatus status, Pageable pageable) {
        String normalized = (search == null) ? "" : search.trim();
        Page<Loan> page = (status == null)
                ? loanRepository.searchByText(normalized, pageable)
                : loanRepository.searchByTextAndStatus(normalized, status, pageable);
        return PageResponse.from(page, loanMapper::toResponse);
    }
}