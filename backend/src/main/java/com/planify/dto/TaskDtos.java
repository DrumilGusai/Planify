package com.planify.dto;

import com.planify.entity.TaskPriority;
import com.planify.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class TaskDtos {

    private TaskDtos() {}

    public record CreateTaskReq(
            @NotBlank(message = "Title is required")
            @Size(max = 200, message = "Title must be at most 200 characters")
            String title,

            @Size(max = 20000, message = "Description must be at most 20000 characters")
            String description,

            TaskPriority priority,
            Long assigneeId,
            LocalDate dueDate
    ) {}

    public record UpdateTaskReq(
            @NotBlank(message = "Title is required")
            @Size(max = 200, message = "Title must be at most 200 characters")
            String title,

            @Size(max = 20000, message = "Description must be at most 20000 characters")
            String description,

            @NotNull(message = "Status is required")
            TaskStatus status,

            @NotNull(message = "Priority is required")
            TaskPriority priority,

            Long assigneeId,
            LocalDate dueDate
    ) {}

    public record StatusReq(
            @NotNull(message = "Status is required")
            TaskStatus status
    ) {}

    public record TaskDto(
            Long id,
            Long workspaceId,
            Long projectId,
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            Long assigneeId,
            String assigneeName,
            LocalDate dueDate,
            Long createdBy,
            Instant createdAt,
            Instant updatedAt
    ) implements AuditContext {
        @Override
        public Long entityId() {
            return id;
        }
    }

    public record TaskFilter(
            TaskStatus status,
            TaskPriority priority,
            Long assigneeId,
            LocalDate dueFrom,
            LocalDate dueTo,
            String q
    ) {}

    public record PageRes<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
