package com.planify.security;

import com.planify.cache.SafeCache;
import com.planify.entity.PlatformRole;
import com.planify.entity.Project;
import com.planify.entity.ProjectRole;
import com.planify.entity.Task;
import com.planify.entity.WorkspaceRole;
import com.planify.repository.ProjectMemberRepository;
import com.planify.repository.ProjectRepository;
import com.planify.repository.TaskRepository;
import com.planify.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock private WorkspaceMemberRepository workspaceMembers;
    @Mock private ProjectMemberRepository projectMembers;
    @Mock private ProjectRepository projects;
    @Mock private TaskRepository tasks;
    @Mock private SafeCache cache;

    @InjectMocks
    private PermissionService permissionService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, PlatformRole role) {
        var principal = new AuthPrincipal(userId, "u" + userId + "@planify.local", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @SuppressWarnings("unchecked")
    private void stubRoles(WorkspaceRole wsRole, ProjectRole prRole) {
        lenient().when(cache.getOrNull(anyString(), any(), anyLong(), any()))
                .thenAnswer(inv -> {
                    Class<?> cls = inv.getArgument(1);
                    if (cls == WorkspaceRole.class) return wsRole;
                    if (cls == ProjectRole.class) return prRole;
                    return ((Supplier<?>) inv.getArgument(3)).get();
                });
    }

    @Test
    void superAdminHasFullAccess() {
        authenticate(1L, PlatformRole.SUPER_ADMIN);
        assertThat(permissionService.isWorkspaceOwner(99L)).isTrue();
        assertThat(permissionService.canManageProject(55L)).isTrue();
        assertThat(permissionService.canViewAudit(99L)).isTrue();
    }

    @Test
    void workspaceAdminCanManageProjectsButNotDeleteWorkspace() {
        authenticate(2L, PlatformRole.USER);
        stubRoles(WorkspaceRole.ADMIN, null);

        Project p = new Project();
        p.setId(10L);
        p.setWorkspaceId(5L);
        when(projects.findById(10L)).thenReturn(Optional.of(p));

        assertThat(permissionService.isWorkspaceOwner(5L)).isFalse();
        assertThat(permissionService.canManageWorkspace(5L)).isTrue();
        assertThat(permissionService.canManageProject(10L)).isTrue();
        assertThat(permissionService.canViewAudit(5L)).isTrue();
    }

    @Test
    void contributorCanEditTasksAndDeleteOnlyOwnTasks() {
        authenticate(3L, PlatformRole.USER);
        stubRoles(WorkspaceRole.MEMBER, ProjectRole.CONTRIBUTOR);

        Project p = new Project();
        p.setId(10L);
        p.setWorkspaceId(5L);
        when(projects.findById(10L)).thenReturn(Optional.of(p));

        Task ownTask = new Task();
        ownTask.setId(100L);
        ownTask.setProjectId(10L);
        ownTask.setCreatedBy(3L);

        Task otherTask = new Task();
        otherTask.setId(101L);
        otherTask.setProjectId(10L);
        otherTask.setCreatedBy(99L);

        when(tasks.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(ownTask));
        when(tasks.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(otherTask));

        assertThat(permissionService.canEditTask(10L)).isTrue();
        assertThat(permissionService.canManageProject(10L)).isFalse();
        assertThat(permissionService.canDeleteTask(100L)).isTrue();
        assertThat(permissionService.canDeleteTask(101L)).isFalse();
        assertThat(permissionService.canViewProjectAudit(10L)).isFalse();
    }

    @Test
    void viewerIsReadOnly() {
        authenticate(4L, PlatformRole.USER);
        stubRoles(WorkspaceRole.MEMBER, ProjectRole.VIEWER);

        Project p = new Project();
        p.setId(10L);
        p.setWorkspaceId(5L);
        when(projects.findById(10L)).thenReturn(Optional.of(p));

        assertThat(permissionService.canViewProject(10L)).isTrue();
        assertThat(permissionService.canEditTask(10L)).isFalse();
        assertThat(permissionService.canManageProject(10L)).isFalse();
    }
}
