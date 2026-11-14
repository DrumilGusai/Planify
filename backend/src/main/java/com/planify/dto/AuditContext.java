package com.planify.dto;

public interface AuditContext {
    Long workspaceId();
    Long projectId();
    Long entityId();
}
