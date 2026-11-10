package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.request.CreateLoanTypeRequest;
import com.example.loanmanagement.dto.request.UpdateLoanTypeRequest;
import com.example.loanmanagement.dto.response.LoanTypeResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.enums.LoanTypeStatus;
import org.springframework.data.domain.Pageable;

public interface LoanTypeService {
    LoanTypeResponse create(CreateLoanTypeRequest request);
    LoanTypeResponse update(Long id, UpdateLoanTypeRequest request);
    LoanTypeResponse changeStatus(Long id, LoanTypeStatus newStatus);
    LoanTypeResponse findById(Long id);
    PageResponse<LoanTypeResponse> findAll(String search, LoanTypeStatus status, Pageable pageable);
}