package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.response.AuditLogResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void record(String action, String entityType, Long entityId, String description);
    PageResponse<AuditLogResponse> getAuditLogs(Pageable pageable);
}