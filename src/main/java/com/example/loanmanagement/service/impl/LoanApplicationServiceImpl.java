package com.example.loanmanagement.service.impl;

import com.example.loanmanagement.dto.request.AdminDecisionRequest;
import com.example.loanmanagement.dto.request.CreateLoanApplicationRequest;
import com.example.loanmanagement.dto.request.RecommendApplicationRequest;
import com.example.loanmanagement.dto.request.UpdateLoanApplicationRequest;
import com.example.loanmanagement.dto.response.LoanApplicationResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.entity.*;
import com.example.loanmanagement.enums.ApplicationStatus;
import com.example.loanmanagement.enums.CustomerStatus;
import com.example.loanmanagement.enums.LoanTypeStatus;
import com.example.loanmanagement.exception.BusinessRuleException;
import com.example.loanmanagement.exception.InvalidStateTransitionException;
import com.example.loanmanagement.exception.ResourceNotFoundException;
import com.example.loanmanagement.mapper.LoanApplicationMapper;
import com.example.loanmanagement.repository.ApplicationHistoryRepository;
import com.example.loanmanagement.repository.CustomerRepository;
import com.example.loanmanagement.repository.LoanApplicationRepository;
import com.example.loanmanagement.repository.LoanRepository;
import com.example.loanmanagement.repository.LoanTypeRepository;
import com.example.loanmanagement.repository.UserRepository;
import com.example.loanmanagement.service.AuditLogService;
import com.example.loanmanagement.service.LoanApplicationService;
import com.example.loanmanagement.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationServiceImpl.class);

    private final LoanApplicationRepository applicationRepository;
    private final CustomerRepository customerRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final UserRepository userRepository;
    private final LoanApplicationMapper mapper;
    private final ApplicationHistoryRepository historyRepository;
    private final AuditLogService auditLogService;
    private final LoanRepository loanRepository; // <-- NEW

    public LoanApplicationServiceImpl(LoanApplicationRepository applicationRepository,
                                      CustomerRepository customerRepository,
                                      LoanTypeRepository loanTypeRepository,
                                      UserRepository userRepository,
                                      LoanApplicationMapper mapper,
                                      ApplicationHistoryRepository historyRepository,
                                      AuditLogService auditLogService,
                                      LoanRepository loanRepository) { // <-- NEW
        this.applicationRepository = applicationRepository;
        this.customerRepository = customerRepository;
        this.loanTypeRepository = loanTypeRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.historyRepository = historyRepository;
        this.auditLogService = auditLogService;
        this.loanRepository = loanRepository; // <-- NEW
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public LoanApplicationResponse create(CreateLoanApplicationRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", "id", request.getCustomerId()));
        
        if (customer.getCustomerStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot create application for an inactive customer");
        }

        LoanType loanType = loanTypeRepository.findById(request.getLoanTypeId())
                .orElseThrow(() -> ResourceNotFoundException.of("LoanType", "id", request.getLoanTypeId()));

        if (loanType.getLoanTypeStatus() != LoanTypeStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot create application for an inactive loan type");
        }

        if (request.getRequestedAmount().compareTo(loanType.getMinimumAmount()) < 0 ||
            request.getRequestedAmount().compareTo(loanType.getMaximumAmount()) > 0) {
            throw new BusinessRuleException("Requested amount must be between " 
                    + loanType.getMinimumAmount() + " and " + loanType.getMaximumAmount());
        }
        if (request.getRequestedTenureMonths() < loanType.getMinimumTenureMonths() ||
            request.getRequestedTenureMonths() > loanType.getMaximumTenureMonths()) {
            throw new BusinessRuleException("Requested tenure must be between " 
                    + loanType.getMinimumTenureMonths() + " and " + loanType.getMaximumTenureMonths() + " months");
        }

        User currentUser = getCurrentUser();
        long nextSeq = applicationRepository.findMaxApplicationSequence() + 1;
        String appNumber = IdGenerator.applicationNumber(YearMonth.now(), nextSeq);

        LoanApplication app = LoanApplication.builder()
                .applicationNumber(appNumber)
                .customer(customer)
                .loanType(loanType)
                .requestedAmount(request.getRequestedAmount())
                .annualInterestRate(loanType.getAnnualInterestRate())
                .requestedTenureMonths(request.getRequestedTenureMonths())
                .purpose(request.getPurpose())
                .applicationStatus(ApplicationStatus.DRAFT)
                .applicationDate(LocalDate.now())
                .createdBy(currentUser)
                .build();

        LoanApplication saved = applicationRepository.save(app);
        logHistory(saved, null, ApplicationStatus.DRAFT, "Application created as DRAFT");
        auditLogService.record("APPLICATION_CREATED", "LoanApplication", saved.getId(), "Draft created: " + appNumber);
        
        return mapper.toResponse(saved, false); // Newly created, not disbursed
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public LoanApplicationResponse update(Long id, UpdateLoanApplicationRequest request) {
        LoanApplication app = getApp(id);

        if (app.getApplicationStatus() != ApplicationStatus.DRAFT) {
            throw InvalidStateTransitionException.between("LoanApplication", app.getApplicationStatus(), "UPDATE");
        }

        LoanType loanType = app.getLoanType();
        if (request.getRequestedAmount().compareTo(loanType.getMinimumAmount()) < 0 ||
            request.getRequestedAmount().compareTo(loanType.getMaximumAmount()) > 0) {
            throw new BusinessRuleException("Requested amount must be between " 
                    + loanType.getMinimumAmount() + " and " + loanType.getMaximumAmount());
        }
        if (request.getRequestedTenureMonths() < loanType.getMinimumTenureMonths() ||
            request.getRequestedTenureMonths() > loanType.getMaximumTenureMonths()) {
            throw new BusinessRuleException("Requested tenure must be between " 
                    + loanType.getMinimumTenureMonths() + " and " + loanType.getMaximumTenureMonths() + " months");
        }

        app.setRequestedAmount(request.getRequestedAmount());
        app.setRequestedTenureMonths(request.getRequestedTenureMonths());
        app.setPurpose(request.getPurpose());
        
        return mapper.toResponse(applicationRepository.save(app), false);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public LoanApplicationResponse submit(Long id) {
        LoanApplication app = getApp(id);
        
        if (app.getApplicationStatus() != ApplicationStatus.DRAFT) {
            throw InvalidStateTransitionException.between("LoanApplication", app.getApplicationStatus(), ApplicationStatus.SUBMITTED);
        }

        ApplicationStatus oldStatus = app.getApplicationStatus();
        app.setApplicationStatus(ApplicationStatus.SUBMITTED);
        
        logHistory(app, oldStatus, ApplicationStatus.SUBMITTED, "Application submitted for review");
        auditLogService.record("APPLICATION_SUBMITTED", "LoanApplication", app.getId(), "Application submitted");
        
        return mapper.toResponse(applicationRepository.save(app), false);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public LoanApplicationResponse startReview(Long id) {
        LoanApplication app = getApp(id);
        
        if (app.getApplicationStatus() != ApplicationStatus.SUBMITTED) {
            throw InvalidStateTransitionException.between("LoanApplication", app.getApplicationStatus(), ApplicationStatus.UNDER_REVIEW);
        }

        ApplicationStatus oldStatus = app.getApplicationStatus();
        app.setApplicationStatus(ApplicationStatus.UNDER_REVIEW);
        app.setReviewedBy(getCurrentUser());
        
        logHistory(app, oldStatus, ApplicationStatus.UNDER_REVIEW, "Review started by Loan Officer");
        
        return mapper.toResponse(applicationRepository.save(app), false);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public LoanApplicationResponse recommend(Long id, RecommendApplicationRequest request) {
        LoanApplication app = getApp(id);
        
        if (app.getApplicationStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw InvalidStateTransitionException.between("LoanApplication", app.getApplicationStatus(), ApplicationStatus.RECOMMENDED);
        }

        ApplicationStatus oldStatus = app.getApplicationStatus();
        app.setApplicationStatus(ApplicationStatus.RECOMMENDED);
        app.setRecommendedDecision(request.getDecision());
        app.setOfficerRemarks(request.getOfficerRemarks());
        
        logHistory(app, oldStatus, ApplicationStatus.RECOMMENDED, request.getOfficerRemarks());
        auditLogService.record("APPLICATION_RECOMMENDED", "LoanApplication", app.getId(), 
                "Recommended: " + request.getDecision());
        
        return mapper.toResponse(applicationRepository.save(app), false);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public LoanApplicationResponse decide(Long id, AdminDecisionRequest request) {
        LoanApplication app = getApp(id);
        
        if (app.getApplicationStatus() != ApplicationStatus.RECOMMENDED) {
            throw InvalidStateTransitionException.between("LoanApplication", app.getApplicationStatus(), "FINAL_DECISION");
        }

        if (request.getDecision() != ApplicationStatus.APPROVED && request.getDecision() != ApplicationStatus.REJECTED) {
            throw new BusinessRuleException("Decision must be either APPROVED or REJECTED");
        }

        ApplicationStatus oldStatus = app.getApplicationStatus();
        app.setApplicationStatus(request.getDecision());
        app.setAdminRemarks(request.getAdminRemarks());
        app.setDecidedBy(getCurrentUser());
        app.setDecisionDate(LocalDateTime.now());
        
        logHistory(app, oldStatus, request.getDecision(), request.getAdminRemarks());
        auditLogService.record("LOAN_" + request.getDecision().name(), "LoanApplication", app.getId(), 
                "Final decision: " + request.getDecision() + " by Admin");
        
        return mapper.toResponse(applicationRepository.save(app), false);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','CASHIER')")
    public LoanApplicationResponse findById(Long id) {
        LoanApplication app = getApp(id);
        return mapper.toResponse(app, loanRepository.existsByLoanApplicationId(app.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','CASHIER')")
    public PageResponse<LoanApplicationResponse> findAll(String search, ApplicationStatus status, Pageable pageable) {
        String normalized = (search == null) ? "" : search.trim();
        Page<LoanApplication> page = (status == null)
                ? applicationRepository.searchByText(normalized, pageable)
                : applicationRepository.searchByTextAndStatus(normalized, status, pageable);
        
        return PageResponse.from(page, app -> mapper.toResponse(app, loanRepository.existsByLoanApplicationId(app.getId())));
    }

    private LoanApplication getApp(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("LoanApplication", "id", id));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private void logHistory(LoanApplication app, ApplicationStatus from, ApplicationStatus to, String remarks) {
        ApplicationHistory history = ApplicationHistory.builder()
                .loanApplication(app)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(getCurrentUser())
                .remarks(remarks)
                .build();
        historyRepository.save(history);
    }
}