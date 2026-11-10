package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.request.CreateCustomerRequest;
import com.example.loanmanagement.dto.request.UpdateCustomerRequest;
import com.example.loanmanagement.dto.request.UpdateCustomerStatusRequest;
import com.example.loanmanagement.dto.response.ApiResponse;
import com.example.loanmanagement.dto.response.CustomerResponse;
import com.example.loanmanagement.dto.response.CustomerSummaryResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.enums.CustomerStatus;
import com.example.loanmanagement.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

/**
 * REST endpoints for Customer Management.
 *
 * Endpoints:
 *   GET    /api/customers                → paginated + searchable list  (all roles)
 *   POST   /api/customers                → register new customer        (ADMIN, LOAN_OFFICER)
 *   GET    /api/customers/{id}           → customer detail              (all roles)
 *   PUT    /api/customers/{id}           → update customer              (ADMIN, LOAN_OFFICER)
 *   PATCH  /api/customers/{id}/status    → toggle status                (ADMIN, LOAN_OFFICER)
 *
 * Design notes:
 *  - Thin controller: no business logic, no try/catch. Failures propagate
 *    as typed exceptions and the GlobalExceptionHandler maps them.
 *  - Authorization is on the SERVICE layer (@PreAuthorize on CustomerServiceImpl).
 *    Duplicating it here would be redundant and a maintenance hazard.
 *  - Pagination is clamped: page >= 0, 1 <= size <= 100.
 *  - Sort field is whitelisted — no path from client input to arbitrary fields.
 *  - Entities never cross this boundary — only DTOs.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    /** Maximum page size a client can request. Protects the DB from DoS. */
    private static final int MAX_PAGE_SIZE = 100;

    /** Fields a client is permitted to sort by. */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "customerCode",
            "firstName",
            "lastName",
            "email",
            "phoneNumber",
            "createdAt",
            "customerStatus"
    );

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // ─────────────────────────────────────────────
    // LIST (paginated + searchable)
    // ─────────────────────────────────────────────

    /**
     * Query params:
     *   search   — optional text filter (name, NIC, phone, email, code)
     *   status   — optional status filter (ACTIVE / INACTIVE)
     *   page     — 0-based page index (default 0)
     *   size     — page size 1..100 (default 10)
     *   sortBy   — whitelisted field name (default "createdAt")
     *   sortDir  — "asc" or "desc" (default "desc")
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerSummaryResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        PageResponse<CustomerSummaryResponse> result =
                customerService.findAll(search, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
            @Valid @RequestBody CreateCustomerRequest request) {

        CustomerResponse created = customerService.create(request);

        // Location header points at the new resource — REST convention for 201.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success("Customer created successfully", created));
    }

    // ─────────────────────────────────────────────
    // DETAIL
    // ─────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.findById(id)));
    }

    // ─────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {

        CustomerResponse updated = customerService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", updated));
    }

    // ─────────────────────────────────────────────
    // STATUS CHANGE
    // ─────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CustomerResponse>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerStatusRequest request) {

        CustomerResponse updated = customerService.changeStatus(id, request.getStatus());
        return ResponseEntity.ok(
                ApiResponse.success("Customer status updated to " + request.getStatus(), updated));
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    /**
     * Build a PageRequest with clamped size, safe page index, whitelisted
     * sort field, and safely-resolved direction (invalid → DESC).
     */
    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }
}