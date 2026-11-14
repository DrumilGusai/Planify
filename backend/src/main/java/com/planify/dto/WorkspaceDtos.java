package com.planify.dto;

import com.planify.entity.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class WorkspaceDtos {

    private WorkspaceDtos() {}

    public record CreateWorkspaceReq(
            @NotBlank(message = "Workspace name is required")
            @Size(max = 100, message = "Workspace name must be at most 100 characters")
            String name,

            @Size(max = 500, message = "Description must be at most 500 characters")
            String description
    ) {}

    public record UpdateWorkspaceReq(
            @NotBlank(message = "Workspace name is required")
            @Size(max = 100, message = "Workspace name must be at most 100 characters")
            String name,

            @Size(max = 500, message = "Description must be at most 500 characters")
            String description
    ) {}

    public record WorkspaceDto(
            Long id,
            String name,
            String description,
            Long ownerId,
            WorkspaceRole myRole,
            Instant createdAt,
            Instant updatedAt
    ) implements AuditContext {
        @Override
        public Long workspaceId() {
            return id;
        }

        @Override
        public Long projectId() {
            return null;
        }

        @Override
        public Long entityId() {
            return id;
        }
    }

    public record AddWorkspaceMemberReq(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotNull(message = "Role is required")
            WorkspaceRole role
    ) {}

    public record UpdateWorkspaceMemberRoleReq(
            @NotNull(message = "Role is required")
            WorkspaceRole role
    ) {}

    public record TransferOwnershipReq(
            @NotNull(message = "New owner userId is required")
            Long newOwnerId
    ) {}

    public record WorkspaceMemberDto(
            Long id,
            Long workspaceId,
            Long userId,
            String name,
            String email,
            WorkspaceRole role,
            Instant joinedAt
    ) implements AuditContext {
        @Override
        public Long projectId() {
            return null;
        }

        @Override
        public Long entityId() {
            return userId;
        }
    }
}
