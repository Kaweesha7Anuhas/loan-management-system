package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.request.AdminDecisionRequest;
import com.example.loanmanagement.dto.request.CreateLoanApplicationRequest;
import com.example.loanmanagement.dto.request.RecommendApplicationRequest;
import com.example.loanmanagement.dto.request.UpdateLoanApplicationRequest;
import com.example.loanmanagement.dto.response.LoanApplicationResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.enums.ApplicationStatus;
import org.springframework.data.domain.Pageable;

public interface LoanApplicationService {
    LoanApplicationResponse create(CreateLoanApplicationRequest request);
    LoanApplicationResponse update(Long id, UpdateLoanApplicationRequest request);
    
    // State Machine Transitions
    LoanApplicationResponse submit(Long id);
    LoanApplicationResponse startReview(Long id);
    LoanApplicationResponse recommend(Long id, RecommendApplicationRequest request);
    LoanApplicationResponse decide(Long id, AdminDecisionRequest request);
    
    LoanApplicationResponse findById(Long id);
    PageResponse<LoanApplicationResponse> findAll(String search, ApplicationStatus status, Pageable pageable);
}