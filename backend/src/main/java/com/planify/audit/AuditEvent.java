package com.planify.audit;

import java.time.Instant;

public record AuditEvent(
        Long actorId,
        Long workspaceId,
        Long projectId,
        String entityType,
        Long entityId,
        String action,
        String oldValue,
        String newValue,
        String ipAddress,
        Instant createdAt
) {}
