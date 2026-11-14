package com.planify.service;

import com.planify.audit.Audited;
import com.planify.cache.SafeCache;
import com.planify.dto.ProjectDtos.AddProjectMemberReq;
import com.planify.dto.ProjectDtos.CreateProjectReq;
import com.planify.dto.ProjectDtos.ProjectDto;
import com.planify.dto.ProjectDtos.ProjectMemberDto;
import com.planify.dto.ProjectDtos.UpdateProjectReq;
import com.planify.entity.PlatformRole;
import com.planify.entity.Project;
import com.planify.entity.ProjectMember;
import com.planify.entity.ProjectRole;
import com.planify.entity.ProjectStatus;
import com.planify.entity.User;
import com.planify.entity.WorkspaceRole;
import com.planify.exception.BadRequestException;
import com.planify.exception.ConflictException;
import com.planify.exception.NotFoundException;
import com.planify.mapper.ProjectMapper;
import com.planify.repository.ProjectMemberRepository;
import com.planify.repository.ProjectRepository;
import com.planify.repository.UserRepository;
import com.planify.repository.WorkspaceMemberRepository;
import com.planify.repository.WorkspaceRepository;
import com.planify.security.AuthPrincipal;
import com.planify.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projects;
    private final ProjectMemberRepository projectMembers;
    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository workspaceMembers;
    private final UserRepository users;
    private final ProjectMapper mapper;
    private final PermissionService permissionService;
    private final SafeCache cache;

    @Transactional
    @Audited(entity = "PROJECT", action = "CREATE")
    public ProjectDto create(Long wsId, CreateProjectReq req, Long userId) {
        if (!workspaces.existsById(wsId)) {
            throw new NotFoundException("Workspace not found");
        }
        String name = req.name().trim();
        if (projects.existsByWorkspaceIdAndName(wsId, name)) {
            throw new ConflictException("A project with this name already exists in the workspace");
        }

        Project project = new Project();
        project.setWorkspaceId(wsId);
        project.setName(name);
        project.setDescription(req.description());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy(userId);
        Project saved = projects.save(project);

        projectMembers.save(new ProjectMember(saved.getId(), userId, ProjectRole.PROJECT_MANAGER, Instant.now()));
        permissionService.evictProjectRole(userId, saved.getId());

        return mapper.toDto(saved, ProjectRole.PROJECT_MANAGER);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> list(Long wsId, AuthPrincipal principal) {
        if (!workspaces.existsById(wsId)) {
            throw new NotFoundException("Workspace not found");
        }
        boolean seeAll = principal.role() == PlatformRole.SUPER_ADMIN;
        if (!seeAll) {
            WorkspaceRole wsRole = permissionService.wsRole(wsId);
            seeAll = wsRole == WorkspaceRole.OWNER || wsRole == WorkspaceRole.ADMIN;
        }
        List<Project> list = seeAll
                ? projects.findByWorkspaceIdOrderByCreatedAtDesc(wsId)
                : projects.findVisibleForMember(wsId, principal.userId());

        return list.stream()
                .map(p -> mapper.toDto(p, permissionService.effectiveProjectRole(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto get(Long pid) {
        Project p = load(pid);
        return mapper.toDto(p, permissionService.effectiveProjectRole(pid));
    }

    @Transactional
    @Audited(entity = "PROJECT", action = "UPDATE")
    public ProjectDto update(Long pid, UpdateProjectReq req) {
        Project p = load(pid);
        String newName = req.name().trim();
        if (!p.getName().equalsIgnoreCase(newName) && projects.existsByWorkspaceIdAndName(p.getWorkspaceId(), newName)) {
            throw new ConflictException("A project with this name already exists in the workspace");
        }
        p.setName(newName);
        p.setDescription(req.description());
        p.setStatus(req.status());
        Project saved = projects.save(p);
        return mapper.toDto(saved, permissionService.effectiveProjectRole(pid));
    }

    @Transactional
    @Audited(entity = "PROJECT", action = "DELETE")
    public void delete(Long pid) {
        Project p = load(pid);
        List<ProjectMember> members = projectMembers.findByProjectId(pid);
        projects.delete(p);
        cache.bump(pid);
        for (ProjectMember m : members) {
            permissionService.evictProjectRole(m.getUserId(), pid);
        }
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberDto> listMembers(Long pid) {
        load(pid);
        List<ProjectMember> members = projectMembers.findByProjectId(pid);
        Map<Long, User> userMap = users.findAllById(
                        members.stream().map(ProjectMember::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Long wsId = load(pid).getWorkspaceId();

        return members.stream()
                .map(m -> toMemberDto(m, wsId, userMap.get(m.getUserId())))
                .toList();
    }

    @Transactional
    @Audited(entity = "MEMBER", action = "CREATE")
    public ProjectMemberDto addMember(Long pid, AddProjectMemberReq req) {
        Project project = load(pid);
        if (!workspaceMembers.existsByWorkspaceIdAndUserId(project.getWorkspaceId(), req.userId())) {
            throw new BadRequestException("User must be a member of the workspace before joining a project");
        }
        if (projectMembers.existsByProjectIdAndUserId(pid, req.userId())) {
            throw new ConflictException("User is already a member of this project");
        }
        User user = users.findById(req.userId())
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("User not found"));

        ProjectMember saved = projectMembers.save(
                new ProjectMember(pid, req.userId(), req.role(), Instant.now())
        );
        permissionService.evictProjectRole(req.userId(), pid);
        return toMemberDto(saved, project.getWorkspaceId(), user);
    }

    @Transactional
    @Audited(entity = "MEMBER", action = "ROLE_CHANGE")
    public ProjectMemberDto updateMemberRole(Long pid, Long userId, ProjectRole role) {
        Project project = load(pid);
        ProjectMember member = projectMembers.findByProjectIdAndUserId(pid, userId)
                .orElseThrow(() -> new NotFoundException("Project member not found"));
        member.setRole(role);
        ProjectMember saved = projectMembers.save(member);
        permissionService.evictProjectRole(userId, pid);

        User user = users.findById(userId).orElse(null);
        return toMemberDto(saved, project.getWorkspaceId(), user);
    }

    @Transactional
    @Audited(entity = "MEMBER", action = "DELETE")
    public void removeMember(Long pid, Long userId) {
        load(pid);
        ProjectMember member = projectMembers.findByProjectIdAndUserId(pid, userId)
                .orElseThrow(() -> new NotFoundException("Project member not found"));
        projectMembers.delete(member);
        permissionService.evictProjectRole(userId, pid);
    }

    private Project load(Long pid) {
        return projects.findById(pid)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    private ProjectMemberDto toMemberDto(ProjectMember m, Long wsId, User u) {
        return new ProjectMemberDto(
                m.getId(),
                wsId,
                m.getProjectId(),
                m.getUserId(),
                u != null ? u.getName() : null,
                u != null ? u.getEmail() : null,
                m.getRole(),
                m.getAddedAt()
        );
    }
}
