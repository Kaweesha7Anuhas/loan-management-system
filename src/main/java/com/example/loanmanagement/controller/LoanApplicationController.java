package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.request.AdminDecisionRequest;
import com.example.loanmanagement.dto.request.CreateLoanApplicationRequest;
import com.example.loanmanagement.dto.request.RecommendApplicationRequest;
import com.example.loanmanagement.dto.request.UpdateLoanApplicationRequest;
import com.example.loanmanagement.dto.response.ApiResponse;
import com.example.loanmanagement.dto.response.LoanApplicationResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.enums.ApplicationStatus;
import com.example.loanmanagement.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/loan-applications")
public class LoanApplicationController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "applicationNumber", "applicationDate", "requestedAmount", "applicationStatus", "createdAt"
    );

    private final LoanApplicationService applicationService;

    public LoanApplicationController(LoanApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LoanApplicationResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(applicationService.findAll(search, status, pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> create(
            @Valid @RequestBody CreateLoanApplicationRequest request) {
        LoanApplicationResponse created = applicationService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ApiResponse.success("Application created (DRAFT)", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateLoanApplicationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Application updated", applicationService.update(id, request)));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application submitted", applicationService.submit(id)));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> startReview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Review started", applicationService.startReview(id)));
    }

    @PostMapping("/{id}/recommend")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> recommend(
            @PathVariable Long id, @Valid @RequestBody RecommendApplicationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Application recommended", applicationService.recommend(id, request)));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> decide(
            @PathVariable Long id, @Valid @RequestBody AdminDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Final decision recorded", applicationService.decide(id, request)));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }
}