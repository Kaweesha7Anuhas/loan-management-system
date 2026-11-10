package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.request.CreateRepaymentRequest;
import com.example.loanmanagement.dto.response.RepaymentResponse;

import java.util.List;

public interface RepaymentService {
    RepaymentResponse recordPayment(CreateRepaymentRequest request);
    List<RepaymentResponse> getRepaymentsForLoan(Long loanId);
}