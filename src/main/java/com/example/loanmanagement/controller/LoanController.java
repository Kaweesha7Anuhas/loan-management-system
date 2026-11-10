package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.response.ApiResponse;
import com.example.loanmanagement.dto.response.LoanResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.dto.response.RepaymentScheduleResponse;
import com.example.loanmanagement.enums.LoanStatus;
import com.example.loanmanagement.service.LoanService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "loanAccountNumber", "principalAmount", "outstandingBalance", "disbursementDate", "loanStatus"
    );

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LoanResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(loanService.findAll(search, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(loanService.findById(id)));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse<List<RepaymentScheduleResponse>>> getSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getSchedule(id)));
    }

    @PostMapping("/{applicationId}/disburse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponse>> disburse(@PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success("Loan disbursed successfully", loanService.disburse(applicationId)));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }
}