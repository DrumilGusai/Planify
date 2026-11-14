package com.planify.controller;

import com.planify.dto.WorkspaceDtos.AddWorkspaceMemberReq;
import com.planify.dto.WorkspaceDtos.CreateWorkspaceReq;
import com.planify.dto.WorkspaceDtos.TransferOwnershipReq;
import com.planify.dto.WorkspaceDtos.UpdateWorkspaceMemberRoleReq;
import com.planify.dto.WorkspaceDtos.UpdateWorkspaceReq;
import com.planify.dto.WorkspaceDtos.WorkspaceDto;
import com.planify.dto.WorkspaceDtos.WorkspaceMemberDto;
import com.planify.security.AuthPrincipal;
import com.planify.service.WorkspaceService;
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
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<WorkspaceDto> create(
            @Valid @RequestBody CreateWorkspaceReq req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.create(req, principal.userId()));
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceDto>> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(workspaceService.listForUser(principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.isWorkspaceMember(#id)")
    public ResponseEntity<WorkspaceDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(workspaceService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.canManageWorkspace(#id)")
    public ResponseEntity<WorkspaceDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkspaceReq req) {
        return ResponseEntity.ok(workspaceService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.isWorkspaceOwner(#id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workspaceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("@permissionService.isWorkspaceOwner(#id)")
    public ResponseEntity<WorkspaceDto> transferOwnership(
            @PathVariable Long id,
            @Valid @RequestBody TransferOwnershipReq req) {
        return ResponseEntity.ok(workspaceService.transferOwnership(id, req.newOwnerId()));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("@permissionService.isWorkspaceMember(#id)")
    public ResponseEntity<List<WorkspaceMemberDto>> listMembers(@PathVariable Long id) {
        return ResponseEntity.ok(workspaceService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("@permissionService.canManageWorkspace(#id)")
    public ResponseEntity<WorkspaceMemberDto> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddWorkspaceMemberReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.addMember(id, req));
    }

    @PatchMapping("/{id}/members/{userId}")
    @PreAuthorize("@permissionService.canManageWorkspace(#id)")
    public ResponseEntity<WorkspaceMemberDto> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateWorkspaceMemberRoleReq req) {
        return ResponseEntity.ok(workspaceService.updateMemberRole(id, userId, req.role()));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("@permissionService.canManageWorkspace(#id)")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId) {
        workspaceService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }
}
