package com.example.loanmanagement.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class CollectionReportResponse {
    private LocalDate date;
    private long transactionCount;
    private BigDecimal totalCollected;
}