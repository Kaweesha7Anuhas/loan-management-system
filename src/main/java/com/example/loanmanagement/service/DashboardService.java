package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.response.AdminDashboardResponse;
import com.example.loanmanagement.dto.response.CashierDashboardResponse;
import com.example.loanmanagement.dto.response.OfficerDashboardResponse;

public interface DashboardService {
    AdminDashboardResponse getAdminDashboard();
    OfficerDashboardResponse getOfficerDashboard();
    CashierDashboardResponse getCashierDashboard();
}