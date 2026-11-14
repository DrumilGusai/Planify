package com.planify.controller;

import com.planify.dto.AuditDtos.AuditFilter;
import com.planify.dto.AuditDtos.AuditLogDto;
import com.planify.dto.TaskDtos.PageRes;
import com.planify.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/workspaces/{id}/audit-logs")
    @PreAuthorize("@permissionService.canViewAudit(#id)")
    public ResponseEntity<PageRes<AuditLogDto>> workspaceAudit(
            @PathVariable Long id,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditService.listForWorkspace(
                id, new AuditFilter(entityType, actorId, from, to), pageable
        ));
    }

    @GetMapping("/projects/{id}/audit-logs")
    @PreAuthorize("@permissionService.canViewProjectAudit(#id)")
    public ResponseEntity<PageRes<AuditLogDto>> projectAudit(
            @PathVariable Long id,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditService.listForProject(
                id, new AuditFilter(entityType, actorId, from, to), pageable
        ));
    }
}
