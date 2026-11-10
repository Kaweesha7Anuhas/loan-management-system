package com.example.loanmanagement.service.impl;

import com.example.loanmanagement.dto.request.CreateLoanTypeRequest;
import com.example.loanmanagement.dto.request.UpdateLoanTypeRequest;
import com.example.loanmanagement.dto.response.LoanTypeResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.entity.LoanType;
import com.example.loanmanagement.enums.LoanTypeStatus;
import com.example.loanmanagement.exception.BusinessRuleException;
import com.example.loanmanagement.exception.DuplicateResourceException;
import com.example.loanmanagement.exception.ResourceNotFoundException;
import com.example.loanmanagement.mapper.LoanTypeMapper;
import com.example.loanmanagement.repository.LoanTypeRepository;
import com.example.loanmanagement.service.LoanTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanTypeServiceImpl implements LoanTypeService {

    private static final Logger log = LoggerFactory.getLogger(LoanTypeServiceImpl.class);

    private final LoanTypeRepository loanTypeRepository;
    private final LoanTypeMapper loanTypeMapper;

    public LoanTypeServiceImpl(LoanTypeRepository loanTypeRepository, LoanTypeMapper loanTypeMapper) {
        this.loanTypeRepository = loanTypeRepository;
        this.loanTypeMapper = loanTypeMapper;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')") // ONLY Admin can create loan products
    public LoanTypeResponse create(CreateLoanTypeRequest request) {
        String code = request.getLoanTypeCode().trim().toUpperCase();
        String name = request.getLoanTypeName().trim();

        if (loanTypeRepository.existsByLoanTypeCode(code)) {
            throw DuplicateResourceException.of("LoanType", "code", code);
        }
        if (loanTypeRepository.existsByLoanTypeName(name)) {
            throw DuplicateResourceException.of("LoanType", "name", name);
        }

        validateBusinessRules(
                request.getMinimumAmount(), request.getMaximumAmount(),
                request.getMinimumTenureMonths(), request.getMaximumTenureMonths());

        LoanType entity = loanTypeMapper.toEntity(request);
        LoanType saved = loanTypeRepository.save(entity);
        log.info("LoanType created: {} ({})", saved.getLoanTypeCode(), saved.getLoanTypeName());
        return loanTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public LoanTypeResponse update(Long id, UpdateLoanTypeRequest request) {
        LoanType entity = loanTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("LoanType", "id", id));

        validateBusinessRules(
                request.getMinimumAmount(), request.getMaximumAmount(),
                request.getMinimumTenureMonths(), request.getMaximumTenureMonths());

        loanTypeMapper.updateEntity(entity, request);
        LoanType saved = loanTypeRepository.save(entity);
        return loanTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public LoanTypeResponse changeStatus(Long id, LoanTypeStatus newStatus) {
        LoanType entity = loanTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("LoanType", "id", id));

        if (entity.getLoanTypeStatus() != newStatus) {
            entity.setLoanTypeStatus(newStatus);
            loanTypeRepository.save(entity);
        }
        return loanTypeMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','CASHIER')") // Everyone can view
    public LoanTypeResponse findById(Long id) {
        return loanTypeRepository.findById(id)
                .map(loanTypeMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("LoanType", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','CASHIER')")
    public PageResponse<LoanTypeResponse> findAll(String search, LoanTypeStatus status, Pageable pageable) {
        String normalized = (search == null) ? "" : search.trim();
        Page<LoanType> page = (status == null)
                ? loanTypeRepository.searchByText(normalized, pageable)
                : loanTypeRepository.searchByTextAndStatus(normalized, status, pageable);
        return PageResponse.from(page, loanTypeMapper::toResponse);
    }

    private void validateBusinessRules(java.math.BigDecimal minAmt, java.math.BigDecimal maxAmt, Integer minTen, Integer maxTen) {
        if (minAmt.compareTo(maxAmt) > 0) {
            throw new BusinessRuleException("Minimum amount cannot be greater than maximum amount");
        }
        if (minTen > maxTen) {
            throw new BusinessRuleException("Minimum tenure cannot be greater than maximum tenure");
        }
    }
}