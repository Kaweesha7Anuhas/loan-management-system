package com.example.loanmanagement.service.impl;

import com.example.loanmanagement.dto.request.CreateRepaymentRequest;
import com.example.loanmanagement.dto.response.RepaymentResponse;
import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.entity.Repayment;
import com.example.loanmanagement.entity.RepaymentSchedule;
import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.enums.LoanStatus;
import com.example.loanmanagement.enums.PaymentStatus;
import com.example.loanmanagement.exception.BusinessRuleException;
import com.example.loanmanagement.exception.ResourceNotFoundException;
import com.example.loanmanagement.mapper.RepaymentMapper;
import com.example.loanmanagement.repository.LoanRepository;
import com.example.loanmanagement.repository.RepaymentRepository;
import com.example.loanmanagement.repository.RepaymentScheduleRepository;
import com.example.loanmanagement.repository.UserRepository;
import com.example.loanmanagement.service.AuditLogService;
import com.example.loanmanagement.service.RepaymentService;
import com.example.loanmanagement.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RepaymentServiceImpl implements RepaymentService {

    private static final Logger log = LoggerFactory.getLogger(RepaymentServiceImpl.class);

    private final RepaymentRepository repaymentRepository;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final RepaymentMapper repaymentMapper;
    private final AuditLogService auditLogService; // <-- Added

    public RepaymentServiceImpl(RepaymentRepository repaymentRepository,
                                LoanRepository loanRepository,
                                RepaymentScheduleRepository scheduleRepository,
                                UserRepository userRepository,
                                RepaymentMapper repaymentMapper,
                                AuditLogService auditLogService) { // <-- Added
        this.repaymentRepository = repaymentRepository;
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.repaymentMapper = repaymentMapper;
        this.auditLogService = auditLogService; // <-- Added
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public RepaymentResponse recordPayment(CreateRepaymentRequest request) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> ResourceNotFoundException.of("Loan", "id", request.getLoanId()));

        if (loan.getLoanStatus() != LoanStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot record payment for a loan that is not ACTIVE");
        }

        if (request.getPaymentAmount().compareTo(loan.getOutstandingBalance()) > 0) {
            throw new BusinessRuleException("Payment amount exceeds the outstanding balance of " + loan.getOutstandingBalance());
        }

        User currentUser = getCurrentUser();
        
        // 1. Generate Receipt Number
        long nextSeq = repaymentRepository.findMaxReceiptSequence() + 1;
        String receiptNo = IdGenerator.receiptNumber(LocalDate.now().getYear(), nextSeq);

        // 2. Create the Repayment Record
        Repayment repayment = Repayment.builder()
                .receiptNumber(receiptNo)
                .loan(loan)
                .customer(loan.getCustomer())
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now())
                .paymentAmount(request.getPaymentAmount())
                .paymentMethod(request.getPaymentMethod())
                .transactionReference(request.getTransactionReference())
                .receivedBy(currentUser)
                .remarks(request.getRemarks())
                .build();

        // 3. Allocate Payment to Schedule (FIFO - Earliest unpaid first)
        BigDecimal remainingPayment = request.getPaymentAmount();
        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstalmentNumberAsc(loan.getId());

        for (RepaymentSchedule schedule : schedules) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) break;
            if (schedule.getPaymentStatus() == PaymentStatus.PAID) continue;

            BigDecimal scheduleRemaining = schedule.getRemainingAmount();

            if (remainingPayment.compareTo(scheduleRemaining) >= 0) {
                // Payment covers this instalment fully
                schedule.setPaidAmount(schedule.getInstalmentAmount());
                schedule.setRemainingAmount(BigDecimal.ZERO);
                schedule.setPaymentStatus(PaymentStatus.PAID);
                remainingPayment = remainingPayment.subtract(scheduleRemaining);
            } else {
                // Payment partially covers this instalment
                BigDecimal newPaid = schedule.getPaidAmount().add(remainingPayment);
                BigDecimal newRemaining = schedule.getInstalmentAmount().subtract(newPaid);

                schedule.setPaidAmount(newPaid);
                schedule.setRemainingAmount(newRemaining);
                schedule.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
                remainingPayment = BigDecimal.ZERO;
            }
            scheduleRepository.save(schedule);
        }

        // 4. Update Loan Balances
        loan.setTotalPaid(loan.getTotalPaid().add(request.getPaymentAmount()));
        loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(request.getPaymentAmount()));

        // 5. Check if Loan is Completed
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) == 0) {
            loan.setLoanStatus(LoanStatus.COMPLETED);
        }

        loanRepository.save(loan);
        Repayment saved = repaymentRepository.save(repayment);

        // 6. Log the Audit Trail
        auditLogService.record("REPAYMENT_RECORDED", "Repayment", saved.getId(), 
                "Collected " + request.getPaymentAmount() + " for loan " + loan.getLoanAccountNumber());

        log.info("Repayment {} recorded: {} for loan {}", receiptNo, request.getPaymentAmount(), loan.getLoanAccountNumber());
        return repaymentMapper.toResponse(saved);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','CASHIER')")
    public List<RepaymentResponse> getRepaymentsForLoan(Long loanId) {
        if (!loanRepository.existsById(loanId)) {
            throw ResourceNotFoundException.of("Loan", "id", loanId);
        }
        return repaymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId)
                .stream()
                .map(repaymentMapper::toResponse)
                .toList();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}