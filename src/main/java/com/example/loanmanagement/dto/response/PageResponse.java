package com.example.loanmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Standard paginated response wrapper. Used by every list endpoint
 * (customers, loan applications, loans, repayments, audit logs).
 *
 * Why wrap Spring's Page<T> instead of returning it directly?
 *  1. Spring's PageImpl serializes with an unstable internal shape
 *     (it changes between Spring versions, and JSON structure is not
 *     part of its public contract).
 *  2. We want to expose only the fields the frontend actually uses
 *     (content, page, size, totalElements, totalPages, hasNext, hasPrev).
 *  3. We want entity -> DTO mapping in one place via the `from()` method,
 *     so services don't repeat the same mapping boilerplate.
 *
 * Usage in a service:
 *     Page<Customer> page = customerRepository.findAll(pageable);
 *     PageResponse<CustomerResponse> response =
 *         PageResponse.from(page, customerMapper::toResponse);
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content;
    private int page;               // 0-based page index
    private int size;               // page size
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    /**
     * Map a Spring Page<E> to a PageResponse<D> using a mapper function.
     * Generic so it works for every entity / DTO pair in the project.
     */
    public static <E, D> PageResponse<D> from(Page<E> page, Function<E, D> mapper) {
        return PageResponse.<D>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}