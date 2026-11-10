package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.response.*;
import com.example.loanmanagement.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getAdminDashboard()));
    }

    @GetMapping("/officer")
    public ResponseEntity<ApiResponse<OfficerDashboardResponse>> getOfficerDashboard() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getOfficerDashboard()));
    }

    @GetMapping("/cashier")
    public ResponseEntity<ApiResponse<CashierDashboardResponse>> getCashierDashboard() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getCashierDashboard()));
    }
}