package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.request.CreateLoanTypeRequest;
import com.example.loanmanagement.dto.request.UpdateCustomerStatusRequest; // We can reuse this for status
import com.example.loanmanagement.dto.request.UpdateLoanTypeRequest;
import com.example.loanmanagement.dto.request.UpdateLoanTypeStatusRequest;
import com.example.loanmanagement.dto.response.ApiResponse;
import com.example.loanmanagement.dto.response.LoanTypeResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.enums.LoanTypeStatus;
import com.example.loanmanagement.service.LoanTypeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/loan-types")
public class LoanTypeController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "loanTypeCode", "loanTypeName", "annualInterestRate", "createdAt"
    );

    private final LoanTypeService loanTypeService;

    public LoanTypeController(LoanTypeService loanTypeService) {
        this.loanTypeService = loanTypeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LoanTypeResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LoanTypeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "loanTypeCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(loanTypeService.findAll(search, status, pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LoanTypeResponse>> create(@Valid @RequestBody CreateLoanTypeRequest request) {
        LoanTypeResponse created = loanTypeService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ApiResponse.success("Loan type created", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanTypeResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(loanTypeService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanTypeResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateLoanTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Loan type updated", loanTypeService.update(id, request)));
    }

       @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<LoanTypeResponse>> changeStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateLoanTypeStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Loan type status updated", 
                loanTypeService.changeStatus(id, request.getStatus())));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "loanTypeCode";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }
}