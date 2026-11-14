package com.planify.dto;

import java.time.Instant;

public final class AuditDtos {

    private AuditDtos() {}

    public record AuditLogDto(
            Long id,
            Long actorId,
            String actorName,
            String actorEmail,
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

    public record AuditFilter(
            String entityType,
            Long actorId,
            Instant from,
            Instant to
    ) {}
}
