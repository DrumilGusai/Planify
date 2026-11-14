package com.planify.controller;

import com.planify.dto.ProjectDtos.AddProjectMemberReq;
import com.planify.dto.ProjectDtos.CreateProjectReq;
import com.planify.dto.ProjectDtos.ProjectDto;
import com.planify.dto.ProjectDtos.ProjectMemberDto;
import com.planify.dto.ProjectDtos.UpdateProjectMemberRoleReq;
import com.planify.dto.ProjectDtos.UpdateProjectReq;
import com.planify.security.AuthPrincipal;
import com.planify.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/workspaces/{wsId}/projects")
    @PreAuthorize("@permissionService.canCreateProject(#wsId)")
    public ResponseEntity<ProjectDto> create(
            @PathVariable Long wsId,
            @Valid @RequestBody CreateProjectReq req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(wsId, req, principal.userId()));
    }

    @GetMapping("/workspaces/{wsId}/projects")
    @PreAuthorize("@permissionService.isWorkspaceMember(#wsId)")
    public ResponseEntity<List<ProjectDto>> list(
            @PathVariable Long wsId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(projectService.list(wsId, principal));
    }

    @GetMapping("/projects/{id}")
    @PreAuthorize("@permissionService.canViewProject(#id)")
    public ResponseEntity<ProjectDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.get(id));
    }

    @PutMapping("/projects/{id}")
    @PreAuthorize("@permissionService.canManageProject(#id)")
    public ResponseEntity<ProjectDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectReq req) {
        return ResponseEntity.ok(projectService.update(id, req));
    }

    @DeleteMapping("/projects/{id}")
    @PreAuthorize("@permissionService.canManageProject(#id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{id}/members")
    @PreAuthorize("@permissionService.canViewProject(#id)")
    public ResponseEntity<List<ProjectMemberDto>> listMembers(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.listMembers(id));
    }

    @PostMapping("/projects/{id}/members")
    @PreAuthorize("@permissionService.canManageProject(#id)")
    public ResponseEntity<ProjectMemberDto> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddProjectMemberReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.addMember(id, req));
    }

    @PatchMapping("/projects/{id}/members/{userId}")
    @PreAuthorize("@permissionService.canManageProject(#id)")
    public ResponseEntity<ProjectMemberDto> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProjectMemberRoleReq req) {
        return ResponseEntity.ok(projectService.updateMemberRole(id, userId, req.role()));
    }

    @DeleteMapping("/projects/{id}/members/{userId}")
    @PreAuthorize("@permissionService.canManageProject(#id)")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId) {
        projectService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }
}
