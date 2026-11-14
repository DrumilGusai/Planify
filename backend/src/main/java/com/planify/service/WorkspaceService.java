package com.planify.service;

import com.planify.audit.Audited;
import com.planify.cache.SafeCache;
import com.planify.dto.WorkspaceDtos.AddWorkspaceMemberReq;
import com.planify.dto.WorkspaceDtos.CreateWorkspaceReq;
import com.planify.dto.WorkspaceDtos.UpdateWorkspaceReq;
import com.planify.dto.WorkspaceDtos.WorkspaceDto;
import com.planify.dto.WorkspaceDtos.WorkspaceMemberDto;
import com.planify.entity.PlatformRole;
import com.planify.entity.Project;
import com.planify.entity.User;
import com.planify.entity.Workspace;
import com.planify.entity.WorkspaceMember;
import com.planify.entity.WorkspaceRole;
import com.planify.exception.BadRequestException;
import com.planify.exception.ConflictException;
import com.planify.exception.NotFoundException;
import com.planify.mapper.WorkspaceMapper;
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
public class WorkspaceService {

    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository workspaceMembers;
    private final ProjectRepository projects;
    private final ProjectMemberRepository projectMembers;
    private final UserRepository users;
    private final WorkspaceMapper mapper;
    private final PermissionService permissionService;
    private final SafeCache cache;

    @Transactional
    @Audited(entity = "WORKSPACE", action = "CREATE")
    public WorkspaceDto create(CreateWorkspaceReq req, Long userId) {
        Workspace ws = new Workspace();
        ws.setName(req.name().trim());
        ws.setDescription(req.description());
        ws.setOwnerId(userId);
        Workspace saved = workspaces.save(ws);

        workspaceMembers.save(new WorkspaceMember(saved.getId(), userId, WorkspaceRole.OWNER, Instant.now()));
        permissionService.evictWorkspaceRole(userId, saved.getId());
        return mapper.toDto(saved, WorkspaceRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDto> listForUser(AuthPrincipal principal) {
        List<Workspace> list = principal.role() == PlatformRole.SUPER_ADMIN
                ? workspaces.findAll()
                : workspaces.findByMemberUserId(principal.userId());
        return list.stream()
                .map(w -> mapper.toDto(w, permissionService.effectiveWorkspaceRole(w.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceDto get(Long wsId) {
        Workspace ws = load(wsId);
        return mapper.toDto(ws, permissionService.effectiveWorkspaceRole(wsId));
    }

    @Transactional
    @Audited(entity = "WORKSPACE", action = "UPDATE")
    public WorkspaceDto update(Long wsId, UpdateWorkspaceReq req) {
        Workspace ws = load(wsId);
        ws.setName(req.name().trim());
        ws.setDescription(req.description());
        Workspace saved = workspaces.save(ws);
        return mapper.toDto(saved, permissionService.effectiveWorkspaceRole(wsId));
    }

    @Transactional
    @Audited(entity = "WORKSPACE", action = "DELETE")
    public void delete(Long wsId) {
        Workspace ws = load(wsId);
        List<Project> wsProjects = projects.findByWorkspaceIdOrderByCreatedAtDesc(wsId);
        List<WorkspaceMember> members = workspaceMembers.findByWorkspaceId(wsId);

        workspaces.delete(ws);

        for (Project p : wsProjects) {
            cache.bump(p.getId());
            for (WorkspaceMember m : members) {
                permissionService.evictProjectRole(m.getUserId(), p.getId());
            }
        }
        for (WorkspaceMember m : members) {
            permissionService.evictWorkspaceRole(m.getUserId(), wsId);
        }
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberDto> listMembers(Long wsId) {
        load(wsId);
        List<WorkspaceMember> members = workspaceMembers.findByWorkspaceId(wsId);
        Map<Long, User> userMap = users.findAllById(
                        members.stream().map(WorkspaceMember::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return members.stream()
                .map(m -> toMemberDto(m, userMap.get(m.getUserId())))
                .toList();
    }

    @Transactional
    @Audited(entity = "MEMBER", action = "CREATE")
    public WorkspaceMemberDto addMember(Long wsId, AddWorkspaceMemberReq req) {
        load(wsId);
        if (req.role() == WorkspaceRole.OWNER) {
            throw new BadRequestException("Cannot add member as OWNER. Use ownership transfer instead.");
        }
        String email = req.email().trim().toLowerCase();
        User user = users.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("No active user found with email: " + email));

        if (workspaceMembers.existsByWorkspaceIdAndUserId(wsId, user.getId())) {
            throw new ConflictException("User is already a member of this workspace");
        }

        WorkspaceMember saved = workspaceMembers.save(
                new WorkspaceMember(wsId, user.getId(), req.role(), Instant.now())
        );
        permissionService.evictWorkspaceRole(user.getId(), wsId);
        return toMemberDto(saved, user);
    }

    @Transactional
    @Audited(entity = "MEMBER", action = "ROLE_CHANGE")
    public WorkspaceMemberDto updateMemberRole(Long wsId, Long userId, WorkspaceRole role) {
        load(wsId);
        if (role == WorkspaceRole.OWNER) {
            throw new BadRequestException("Cannot assign OWNER role directly. Use ownership transfer instead.");
        }
        WorkspaceMember member = workspaceMembers.findByWorkspaceIdAndUserId(wsId, userId)
                .orElseThrow(() -> new NotFoundException("Workspace member not found"));

        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new BadRequestException("Cannot change role of workspace OWNER. Transfer ownership first.");
        }

        member.setRole(role);
        WorkspaceMember saved = workspaceMembers.save(member);
        permissionService.evictWorkspaceRole(userId, wsId);

        User user = users.findById(userId).orElse(null);
        return toMemberDto(saved, user);
    }

    @Transactional
    @Audited(entity = "MEMBER", action = "DELETE")
    public void removeMember(Long wsId, Long userId) {
        load(wsId);
        WorkspaceMember member = workspaceMembers.findByWorkspaceIdAndUserId(wsId, userId)
                .orElseThrow(() -> new NotFoundException("Workspace member not found"));

        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new BadRequestException("Cannot remove workspace OWNER");
        }

        List<Project> wsProjects = projects.findByWorkspaceIdOrderByCreatedAtDesc(wsId);
        projectMembers.deleteByWorkspaceIdAndUserId(wsId, userId);
        workspaceMembers.delete(member);

        permissionService.evictWorkspaceRole(userId, wsId);
        for (Project p : wsProjects) {
            permissionService.evictProjectRole(userId, p.getId());
        }
    }

    @Transactional
    @Audited(entity = "WORKSPACE", action = "ROLE_CHANGE")
    public WorkspaceDto transferOwnership(Long wsId, Long newOwnerId) {
        Workspace ws = load(wsId);
        if (ws.getOwnerId().equals(newOwnerId)) {
            throw new BadRequestException("User is already the workspace owner");
        }

        WorkspaceMember currentOwner = workspaceMembers.findByWorkspaceIdAndUserId(wsId, ws.getOwnerId())
                .orElseThrow(() -> new IllegalStateException("Workspace owner membership missing"));
        WorkspaceMember nextOwner = workspaceMembers.findByWorkspaceIdAndUserId(wsId, newOwnerId)
                .orElseThrow(() -> new BadRequestException("New owner must already be a workspace member"));

        currentOwner.setRole(WorkspaceRole.ADMIN);
        nextOwner.setRole(WorkspaceRole.OWNER);
        ws.setOwnerId(newOwnerId);

        workspaceMembers.save(currentOwner);
        workspaceMembers.save(nextOwner);
        Workspace saved = workspaces.save(ws);

        permissionService.evictWorkspaceRole(currentOwner.getUserId(), wsId);
        permissionService.evictWorkspaceRole(newOwnerId, wsId);

        return mapper.toDto(saved, permissionService.effectiveWorkspaceRole(wsId));
    }

    private Workspace load(Long wsId) {
        return workspaces.findById(wsId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));
    }

    private WorkspaceMemberDto toMemberDto(WorkspaceMember m, User u) {
        return new WorkspaceMemberDto(
                m.getId(),
                m.getWorkspaceId(),
                m.getUserId(),
                u != null ? u.getName() : null,
                u != null ? u.getEmail() : null,
                m.getRole(),
                m.getJoinedAt()
        );
    }
}
