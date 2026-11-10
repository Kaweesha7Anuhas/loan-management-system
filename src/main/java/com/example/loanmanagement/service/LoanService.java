package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.response.LoanResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.dto.response.RepaymentScheduleResponse;
import com.example.loanmanagement.enums.LoanStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LoanService {
    LoanResponse disburse(Long applicationId);
    LoanResponse findById(Long id);
    List<RepaymentScheduleResponse> getSchedule(Long loanId);
    PageResponse<LoanResponse> findAll(String search, LoanStatus status, Pageable pageable);
}