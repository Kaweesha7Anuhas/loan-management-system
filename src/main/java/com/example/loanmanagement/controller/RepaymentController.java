package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.request.CreateRepaymentRequest;
import com.example.loanmanagement.dto.response.ApiResponse;
import com.example.loanmanagement.dto.response.RepaymentResponse;
import com.example.loanmanagement.service.RepaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RepaymentController {

    private final RepaymentService repaymentService;

    public RepaymentController(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @PostMapping("/repayments")
    public ResponseEntity<ApiResponse<RepaymentResponse>> recordPayment(
            @Valid @RequestBody CreateRepaymentRequest request) {
        RepaymentResponse response = repaymentService.recordPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Repayment recorded successfully", response));
    }

    @GetMapping("/loans/{loanId}/repayments")
    public ResponseEntity<ApiResponse<List<RepaymentResponse>>> getRepaymentsForLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(repaymentService.getRepaymentsForLoan(loanId)));
    }
}