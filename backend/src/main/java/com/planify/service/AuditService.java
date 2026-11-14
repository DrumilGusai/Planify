package com.planify.service;

import com.planify.audit.AuditSpecs;
import com.planify.dto.AuditDtos.AuditFilter;
import com.planify.dto.AuditDtos.AuditLogDto;
import com.planify.dto.TaskDtos.PageRes;
import com.planify.entity.AuditLog;
import com.planify.entity.User;
import com.planify.exception.NotFoundException;
import com.planify.repository.AuditLogRepository;
import com.planify.repository.ProjectRepository;
import com.planify.repository.UserRepository;
import com.planify.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogs;
    private final WorkspaceRepository workspaces;
    private final ProjectRepository projects;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public PageRes<AuditLogDto> listForWorkspace(Long wsId, AuditFilter filter, Pageable pageable) {
        if (!workspaces.existsById(wsId)) {
            throw new NotFoundException("Workspace not found");
        }
        Pageable pg = normalize(pageable);
        Page<AuditLog> page = auditLogs.findAll(AuditSpecs.forWorkspace(wsId, filter), pg);
        return toPageRes(page);
    }

    @Transactional(readOnly = true)
    public PageRes<AuditLogDto> listForProject(Long pid, AuditFilter filter, Pageable pageable) {
        if (!projects.existsById(pid)) {
            throw new NotFoundException("Project not found");
        }
        Pageable pg = normalize(pageable);
        Page<AuditLog> page = auditLogs.findAll(AuditSpecs.forProject(pid, filter), pg);
        return toPageRes(page);
    }

    private Pageable normalize(Pageable pg) {
        int size = Math.min(Math.max(pg.getPageSize(), 1), 100);
        Sort sort = pg.getSort().isSorted() ? pg.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(Math.max(pg.getPageNumber(), 0), size, sort);
    }

    private PageRes<AuditLogDto> toPageRes(Page<AuditLog> page) {
        Set<Long> actorIds = page.getContent().stream()
                .map(AuditLog::getActorId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = actorIds.isEmpty()
                ? Map.of()
                : users.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<AuditLogDto> content = page.getContent().stream()
                .map(a -> {
                    User u = userMap.get(a.getActorId());
                    return new AuditLogDto(
                            a.getId(),
                            a.getActorId(),
                            u != null ? u.getName() : null,
                            u != null ? u.getEmail() : null,
                            a.getWorkspaceId(),
                            a.getProjectId(),
                            a.getEntityType(),
                            a.getEntityId(),
                            a.getAction(),
                            a.getOldValue(),
                            a.getNewValue(),
                            a.getIpAddress(),
                            a.getCreatedAt()
                    );
                })
                .toList();

        return new PageRes<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
