package com.planify.controller;

import com.planify.dto.TaskDtos.CreateTaskReq;
import com.planify.dto.TaskDtos.PageRes;
import com.planify.dto.TaskDtos.StatusReq;
import com.planify.dto.TaskDtos.TaskDto;
import com.planify.dto.TaskDtos.TaskFilter;
import com.planify.dto.TaskDtos.UpdateTaskReq;
import com.planify.entity.TaskPriority;
import com.planify.entity.TaskStatus;
import com.planify.security.AuthPrincipal;
import com.planify.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projects/{id}/tasks")
    @PreAuthorize("@permissionService.canEditTask(#id)")
    public ResponseEntity<TaskDto> create(
            @PathVariable Long id,
            @Valid @RequestBody CreateTaskReq req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(id, req, principal.userId()));
    }

    @GetMapping("/projects/{id}/tasks")
    @PreAuthorize("@permissionService.canViewProject(#id)")
    public ResponseEntity<PageRes<TaskDto>> list(
            @PathVariable Long id,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        TaskFilter filter = new TaskFilter(status, priority, assigneeId, dueFrom, dueTo, q);
        return ResponseEntity.ok(taskService.list(id, filter, pageable));
    }

    @GetMapping("/tasks/my")
    public ResponseEntity<PageRes<TaskDto>> myTasks(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        TaskFilter filter = new TaskFilter(status, priority, null, dueFrom, dueTo, q);
        return ResponseEntity.ok(taskService.myTasks(principal, filter, pageable));
    }

    @GetMapping("/tasks/{id}")
    @PreAuthorize("@permissionService.canViewTaskById(#id)")
    public ResponseEntity<TaskDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.get(id));
    }

    @PutMapping("/tasks/{id}")
    @PreAuthorize("@permissionService.canEditTaskById(#id)")
    public ResponseEntity<TaskDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskReq req) {
        return ResponseEntity.ok(taskService.update(id, req));
    }

    @PatchMapping("/tasks/{id}/status")
    @PreAuthorize("@permissionService.canEditTaskById(#id)")
    public ResponseEntity<TaskDto> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusReq req) {
        return ResponseEntity.ok(taskService.changeStatus(id, req.status()));
    }

    @DeleteMapping("/tasks/{id}")
    @PreAuthorize("@permissionService.canDeleteTask(#id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
