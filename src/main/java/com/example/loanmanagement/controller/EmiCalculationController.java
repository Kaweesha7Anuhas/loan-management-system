package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.response.ApiResponse;
import com.example.loanmanagement.dto.response.EmiCalculationResponse;
import com.example.loanmanagement.service.EmiCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/loans")
public class EmiCalculationController {

    private final EmiCalculationService emiCalculationService;

    public EmiCalculationController(EmiCalculationService emiCalculationService) {
        this.emiCalculationService = emiCalculationService;
    }

    @GetMapping("/calculate-emi")
    public ResponseEntity<ApiResponse<EmiCalculationResponse>> calculate(
            @RequestParam BigDecimal principal,
            @RequestParam BigDecimal annualRate,
            @RequestParam Integer tenureMonths) {
            
        EmiCalculationResponse response = emiCalculationService.calculateEmi(principal, annualRate, tenureMonths);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}