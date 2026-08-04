package com.transitops.backend.service;

import com.transitops.backend.entity.AuditLog;
import com.transitops.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String actorEmail, String action, String entityType, String entityId, String details) {
        auditLogRepository.save(AuditLog.builder()
                .actorEmail(actorEmail)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }

    public Page<AuditLog> list(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
