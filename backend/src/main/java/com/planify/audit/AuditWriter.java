package com.planify.audit;

import com.planify.entity.AuditLog;
import com.planify.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditWriter {

    private final AuditLogRepository repo;

    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditEvent event) {
        try {
            AuditLog entity = new AuditLog();
            entity.setActorId(event.actorId());
            entity.setWorkspaceId(event.workspaceId());
            entity.setProjectId(event.projectId());
            entity.setEntityType(event.entityType());
            entity.setEntityId(event.entityId());
            entity.setAction(event.action());
            entity.setOldValue(event.oldValue());
            entity.setNewValue(event.newValue());
            entity.setIpAddress(event.ipAddress());
            entity.setCreatedAt(event.createdAt());
            repo.save(entity);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage(), e);
        }
    }
}
