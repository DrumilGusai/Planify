package com.planify.security;

import com.planify.cache.SafeCache;
import com.planify.entity.PlatformRole;
import com.planify.entity.Project;
import com.planify.entity.ProjectMember;
import com.planify.entity.ProjectRole;
import com.planify.entity.Task;
import com.planify.entity.WorkspaceMember;
import com.planify.entity.WorkspaceRole;
import com.planify.exception.NotFoundException;
import com.planify.repository.ProjectMemberRepository;
import com.planify.repository.ProjectRepository;
import com.planify.repository.TaskRepository;
import com.planify.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("permissionService")
@RequiredArgsConstructor
public class PermissionService {

    private static final long PERM_TTL_MIN = 15L;

    private final WorkspaceMemberRepository workspaceMembers;
    private final ProjectMemberRepository projectMembers;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final SafeCache cache;

    public AuthPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return principal;
    }

    public boolean isSuperAdmin() {
        return currentUser().role() == PlatformRole.SUPER_ADMIN;
    }

    public WorkspaceRole wsRole(Long wsId) {
        Long uid = currentUser().userId();
        return cache.getOrNull(
                "perm:%d:w%d".formatted(uid, wsId),
                WorkspaceRole.class,
                PERM_TTL_MIN,
                () -> workspaceMembers.findByWorkspaceIdAndUserId(wsId, uid)
                        .map(WorkspaceMember::getRole)
                        .orElse(null)
        );
    }

    public ProjectRole prRole(Long pid) {
        Long uid = currentUser().userId();
        return cache.getOrNull(
                "perm:%d:p%d".formatted(uid, pid),
                ProjectRole.class,
                PERM_TTL_MIN,
                () -> projectMembers.findByProjectIdAndUserId(pid, uid)
                        .map(ProjectMember::getRole)
                        .orElse(null)
        );
    }

    private boolean isWsAdmin(Long wsId) {
        WorkspaceRole role = wsRole(wsId);
        return role == WorkspaceRole.OWNER || role == WorkspaceRole.ADMIN;
    }

    public Long workspaceOfProject(Long projectId) {
        return projects.findById(projectId)
                .map(Project::getWorkspaceId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    public boolean isWorkspaceMember(Long wsId) {
        return isSuperAdmin() || wsRole(wsId) != null;
    }

    public boolean canManageWorkspace(Long wsId) {
        return isSuperAdmin() || isWsAdmin(wsId);
    }

    public boolean isWorkspaceOwner(Long wsId) {
        return isSuperAdmin() || wsRole(wsId) == WorkspaceRole.OWNER;
    }

    public boolean canCreateProject(Long wsId) {
        return isWorkspaceMember(wsId);
    }

    public boolean canViewProject(Long pid) {
        return isSuperAdmin() || isWsAdmin(workspaceOfProject(pid)) || prRole(pid) != null;
    }

    public boolean canManageProject(Long pid) {
        return isSuperAdmin() || isWsAdmin(workspaceOfProject(pid)) || prRole(pid) == ProjectRole.PROJECT_MANAGER;
    }

    public boolean canEditTask(Long pid) {
        return canManageProject(pid) || prRole(pid) == ProjectRole.CONTRIBUTOR;
    }

    public boolean canViewTaskById(Long taskId) {
        Task task = tasks.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        return canViewProject(task.getProjectId());
    }

    public boolean canEditTaskById(Long taskId) {
        Task task = tasks.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        return canEditTask(task.getProjectId());
    }

    public boolean canDeleteTask(Long taskId) {
        Task task = tasks.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        return canManageProject(task.getProjectId())
                || (prRole(task.getProjectId()) == ProjectRole.CONTRIBUTOR
                && task.getCreatedBy().equals(currentUser().userId()));
    }

    public boolean canViewAudit(Long wsId) {
        return canManageWorkspace(wsId);
    }

    public boolean canViewProjectAudit(Long pid) {
        return canManageProject(pid);
    }

    public ProjectRole effectiveProjectRole(Long pid) {
        if (isSuperAdmin() || isWsAdmin(workspaceOfProject(pid))) {
            return ProjectRole.PROJECT_MANAGER;
        }
        return prRole(pid);
    }

    public WorkspaceRole effectiveWorkspaceRole(Long wsId) {
        if (isSuperAdmin()) {
            return WorkspaceRole.OWNER;
        }
        return wsRole(wsId);
    }

    public void evictWorkspaceRole(Long userId, Long wsId) {
        cache.evict("perm:%d:w%d".formatted(userId, wsId));
    }

    public void evictProjectRole(Long userId, Long pid) {
        cache.evict("perm:%d:p%d".formatted(userId, pid));
    }
}
