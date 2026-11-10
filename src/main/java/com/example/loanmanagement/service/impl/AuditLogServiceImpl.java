package com.example.loanmanagement.service.impl;

import com.example.loanmanagement.dto.response.AuditLogResponse;
import com.example.loanmanagement.dto.response.PageResponse;
import com.example.loanmanagement.entity.AuditLog;
import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.repository.AuditLogRepository;
import com.example.loanmanagement.repository.UserRepository;
import com.example.loanmanagement.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void record(String action, String entityType, Long entityId, String description) {
        String username = SecurityContextHolder.getContext().getAuthentication() != null 
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "SYSTEM";
        
        User user = userRepository.findByUsername(username).orElse(null);

        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build();
        
        auditLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')") // ONLY Admin can view audit logs
    public PageResponse<AuditLogResponse> getAuditLogs(Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        return PageResponse.from(page, log -> AuditLogResponse.builder()
                .id(log.getId())
                .username(log.getUser() != null ? log.getUser().getUsername() : "SYSTEM")
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .createdAt(log.getCreatedAt())
                .build());
    }
}