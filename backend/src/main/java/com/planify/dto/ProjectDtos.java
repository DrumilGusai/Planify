package com.planify.dto;

import com.planify.entity.ProjectRole;
import com.planify.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ProjectDtos {

    private ProjectDtos() {}

    public record CreateProjectReq(
            @NotBlank(message = "Project name is required")
            @Size(max = 100, message = "Project name must be at most 100 characters")
            String name,

            @Size(max = 1000, message = "Description must be at most 1000 characters")
            String description
    ) {}

    public record UpdateProjectReq(
            @NotBlank(message = "Project name is required")
            @Size(max = 100, message = "Project name must be at most 100 characters")
            String name,

            @Size(max = 1000, message = "Description must be at most 1000 characters")
            String description,

            @NotNull(message = "Status is required")
            ProjectStatus status
    ) {}

    public record ProjectDto(
            Long id,
            Long workspaceId,
            String name,
            String description,
            ProjectStatus status,
            Long createdBy,
            ProjectRole myRole,
            Instant createdAt,
            Instant updatedAt
    ) implements AuditContext {
        @Override
        public Long projectId() {
            return id;
        }

        @Override
        public Long entityId() {
            return id;
        }
    }

    public record AddProjectMemberReq(
            @NotNull(message = "User ID is required")
            Long userId,

            @NotNull(message = "Role is required")
            ProjectRole role
    ) {}

    public record UpdateProjectMemberRoleReq(
            @NotNull(message = "Role is required")
            ProjectRole role
    ) {}

    public record ProjectMemberDto(
            Long id,
            Long workspaceId,
            Long projectId,
            Long userId,
            String name,
            String email,
            ProjectRole role,
            Instant addedAt
    ) implements AuditContext {
        @Override
        public Long entityId() {
            return userId;
        }
    }
}
