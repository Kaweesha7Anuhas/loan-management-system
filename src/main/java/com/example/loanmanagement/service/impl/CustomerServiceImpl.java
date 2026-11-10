package com.example.loanmanagement.service.impl;

import com.example.loanmanagement.dto.request.CreateCustomerRequest;
import com.example.loanmanagement.dto.request.UpdateCustomerRequest;
import com.example.loanmanagement.dto.response.CustomerResponse;
import com.example.loanmanagement.dto.response.CustomerSummaryResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.entity.Customer;
import com.example.loanmanagement.enums.CustomerStatus;
import com.example.loanmanagement.exception.DuplicateResourceException;
import com.example.loanmanagement.exception.ResourceNotFoundException;
import com.example.loanmanagement.mapper.CustomerMapper;
import com.example.loanmanagement.repository.CustomerRepository;
import com.example.loanmanagement.service.CustomerService;
import com.example.loanmanagement.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the Customer module.
 *
 * Authorisation is applied BOTH here (via @PreAuthorize) and at the URL
 * level in SecurityConfig. Hiding a button in the frontend is UX;
 * @PreAuthorize is the security boundary. NEVER rely on the frontend alone.
 *
 * The MAX+1 customer code generation retries on collision — see the comment
 * on the `create` method for the full reasoning.
 */
@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    /** How many times to retry on customer-code collision before giving up. */
    private static final int CODE_GENERATION_MAX_RETRIES = 5;

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerRepository customerRepository,
                               CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public CustomerResponse create(CreateCustomerRequest request) {

        // Pre-check duplicates for clean error messages. The unique constraint
        // remains the ultimate arbiter if two requests race past these checks.
        String nic = request.getNicNumber().trim().toUpperCase();
        String email = request.getEmail().trim().toLowerCase();

        if (customerRepository.existsByNicNumber(nic)) {
            throw DuplicateResourceException.of("Customer", "NIC number", nic);
        }
        if (customerRepository.existsByEmail(email)) {
            throw DuplicateResourceException.of("Customer", "email", email);
        }

        // Build the entity from the request (mapper normalises casing and trims).
        Customer customer = customerMapper.toEntity(request);

        // Attempt insert with customer-code retry loop.
        // MAX+1 races under concurrent requests: two callers can read the
        // same max value and produce the same next code. The unique constraint
        // rejects the second insert; we catch that exception and retry with
        // the new max. After CODE_GENERATION_MAX_RETRIES attempts we give up
        // and surface a clean error.
        for (int attempt = 1; attempt <= CODE_GENERATION_MAX_RETRIES; attempt++) {
            long nextSequence = customerRepository.findMaxCustomerSequence() + 1;
            customer.setCustomerCode(IdGenerator.customerCode(nextSequence));

            try {
                Customer saved = customerRepository.save(customer);
                log.info("Customer created: {} ({}) by attempt {}",
                        saved.getCustomerCode(), saved.getFullName(), attempt);
                return customerMapper.toResponse(saved);
            } catch (DataIntegrityViolationException ex) {
                // Could be customerCode collision (retry) OR NIC/email collision
                // (the pre-check raced). Both surface as DataIntegrityViolationException.
                // Distinguishing them requires parsing the message — fragile.
                // Pragmatic approach: retry once, and if the second attempt
                // fails the pre-check was likely wrong (NIC/email race) so
                // rethrow as duplicate.
                log.warn("Insert collision on attempt {} for customer '{}': {}",
                        attempt, customer.getFullName(), ex.getMostSpecificCause().getMessage());

                if (attempt == CODE_GENERATION_MAX_RETRIES) {
                    throw new DuplicateResourceException(
                            "Could not create customer — a duplicate NIC, email, or "
                            + "customer code conflict occurred after multiple attempts");
                }
                // Small sleep lets the competing transaction commit so our
                // next MAX() read sees the new value.
                try {
                    Thread.sleep(20L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new DuplicateResourceException(
                            "Customer creation was interrupted");
                }
            }
        }
        // Unreachable — loop throws on last attempt
        throw new DuplicateResourceException("Could not create customer after retries");
    }

    // ─────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public CustomerResponse update(Long id, UpdateCustomerRequest request) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", "id", id));

        // Email is the only field in this DTO that could conflict with another
        // customer. If it changed and is already taken by someone else → 409.
        String newEmail = request.getEmail().trim().toLowerCase();
        if (!newEmail.equalsIgnoreCase(customer.getEmail())) {
            customerRepository.findByEmail(newEmail).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw DuplicateResourceException.of("Customer", "email", newEmail);
                }
            });
        }

        // Mapper only updates mutable fields (not NIC, not customerCode, not status).
        customerMapper.updateEntity(customer, request);
        Customer saved = customerRepository.save(customer);
        log.info("Customer updated: {} ({})", saved.getCustomerCode(), saved.getFullName());
        return customerMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // STATUS TOGGLE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public CustomerResponse changeStatus(Long id, CustomerStatus newStatus) {

        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus must not be null");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", "id", id));

        if (customer.getCustomerStatus() == newStatus) {
            // Idempotent — no error, no change, same result.
            log.debug("Customer {} already in status {}", customer.getCustomerCode(), newStatus);
            return customerMapper.toResponse(customer);
        }

        customer.setCustomerStatus(newStatus);
        Customer saved = customerRepository.save(customer);
        log.info("Customer status changed: {} ({}) → {}",
                saved.getCustomerCode(), saved.getFullName(), newStatus);
        return customerMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','CASHIER')")
    public CustomerResponse findById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", "id", id));
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','CASHIER')")
    public PageResponse<CustomerSummaryResponse> findAll(
            String search, CustomerStatus status, Pageable pageable) {

        // Normalize search: null / whitespace → empty string, which our
        // repository queries interpret as "no text filter".
        String normalized = (search == null) ? "" : search.trim();

        Page<Customer> page = (status == null)
                ? customerRepository.searchByText(normalized, pageable)
                : customerRepository.searchByTextAndStatus(normalized, status, pageable);

        return PageResponse.from(page, customerMapper::toSummary);
    }
}