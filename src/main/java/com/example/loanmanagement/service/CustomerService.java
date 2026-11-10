package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.request.CreateCustomerRequest;
import com.example.loanmanagement.dto.request.UpdateCustomerRequest;
import com.example.loanmanagement.dto.response.CustomerResponse;
import com.example.loanmanagement.dto.response.CustomerSummaryResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.enums.CustomerStatus;
import org.springframework.data.domain.Pageable;

/**
 * Contract for the Customer module.
 *
 * Controllers depend on THIS interface, not CustomerServiceImpl — so the
 * implementation can be swapped (cached, decorated, mocked) without touching
 * callers.
 *
 * Authorization rules (enforced by @PreAuthorize on the implementation):
 *  - create / update / changeStatus : ADMIN or LOAN_OFFICER
 *  - findAll / findById             : ADMIN, LOAN_OFFICER, CASHIER
 */
public interface CustomerService {

    /**
     * Register a new customer.
     * @throws com.example.loanmanagement.exception.DuplicateResourceException
     *         if NIC or email already exists
     * @throws org.springframework.security.access.AccessDeniedException
     *         if the caller lacks ADMIN / LOAN_OFFICER role
     */
    CustomerResponse create(CreateCustomerRequest request);

    /**
     * Update an existing customer's mutable fields
     * (firstName, lastName, email, phone, address, city, occupation, income).
     * NIC and customerCode are immutable through this operation.
     */
    CustomerResponse update(Long id, UpdateCustomerRequest request);

    /**
     * Toggle customer status (ACTIVE ↔ INACTIVE).
     * Deactivated customers cannot be selected for new loan applications
     * but remain in the database for history.
     */
    CustomerResponse changeStatus(Long id, CustomerStatus newStatus);

    /**
     * Fetch a single customer by ID (full detail).
     */
    CustomerResponse findById(Long id);

    /**
     * Paginated + searchable list.
     *
     * @param search  case-insensitive search across firstName, lastName,
     *                nicNumber, phone, email, customerCode. Empty or null
     *                means "no text filter".
     * @param status  optional status filter. Null means "all statuses".
     * @param pageable pagination + sort instructions from the controller.
     */
    PageResponse<CustomerSummaryResponse> findAll(
            String search, CustomerStatus status, Pageable pageable);
}