package com.planify.audit;

import com.planify.entity.Project;
import com.planify.repository.ProjectMemberRepository;
import com.planify.repository.ProjectRepository;
import com.planify.repository.TaskRepository;
import com.planify.repository.WorkspaceMemberRepository;
import com.planify.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuditSnapshotProvider {

    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository workspaceMembers;
    private final ProjectRepository projects;
    private final ProjectMemberRepository projectMembers;
    private final TaskRepository tasks;

    public record Snapshot(Long workspaceId, Long projectId, Long entityId, Object state) {}

    public Snapshot captureBefore(String entityType, String action, Object[] args) {
        if ("CREATE".equals(action) && !"MEMBER".equals(entityType)) {
            return null;
        }
        try {
            return switch (entityType) {
                case "WORKSPACE" -> {
                    Long wsId = firstLong(args);
                    if (wsId == null) yield null;
                    yield workspaces.findById(wsId)
                            .map(w -> new Snapshot(w.getId(), null, w.getId(), Map.of(
                                    "id", w.getId(),
                                    "name", w.getName(),
                                    "description", w.getDescription() != null ? w.getDescription() : "",
                                    "ownerId", w.getOwnerId())))
                            .orElse(null);
                }
                case "PROJECT" -> {
                    Long pid = firstLong(args);
                    if (pid == null) yield null;
                    yield projects.findById(pid)
                            .map(p -> new Snapshot(p.getWorkspaceId(), p.getId(), p.getId(), Map.of(
                                    "id", p.getId(),
                                    "name", p.getName(),
                                    "description", p.getDescription() != null ? p.getDescription() : "",
                                    "status", p.getStatus().name())))
                            .orElse(null);
                }
                case "TASK" -> {
                    Long tid = firstLong(args);
                    if (tid == null) yield null;
                    yield tasks.findByIdAndDeletedFalse(tid)
                            .map(t -> {
                                Long wsId = projects.findById(t.getProjectId()).map(Project::getWorkspaceId).orElse(null);
                                return new Snapshot(wsId, t.getProjectId(), t.getId(), Map.of(
                                        "id", t.getId(),
                                        "title", t.getTitle(),
                                        "status", t.getStatus().name(),
                                        "priority", t.getPriority().name(),
                                        "assigneeId", t.getAssigneeId() != null ? t.getAssigneeId() : "",
                                        "dueDate", t.getDueDate() != null ? t.getDueDate().toString() : ""
                                ));
                            })
                            .orElse(null);
                }
                case "MEMBER" -> captureMember(args);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private Snapshot captureMember(Object[] args) {
        if (args.length >= 2 && args[0] instanceof Long scopeId && args[1] instanceof Long userId) {
            // Try workspace member vs project member based on method arguments
            var wm = workspaceMembers.findByWorkspaceIdAndUserId(scopeId, userId);
            if (wm.isPresent()) {
                var m = wm.get();
                return new Snapshot(scopeId, null, userId, Map.of(
                        "workspaceId", scopeId,
                        "userId", userId,
                        "role", m.getRole().name()
                ));
            }
            var pm = projectMembers.findByProjectIdAndUserId(scopeId, userId);
            if (pm.isPresent()) {
                var m = pm.get();
                Long wsId = projects.findById(scopeId).map(Project::getWorkspaceId).orElse(null);
                return new Snapshot(wsId, scopeId, userId, Map.of(
                        "projectId", scopeId,
                        "userId", userId,
                        "role", m.getRole().name()
                ));
            }
        }
        return null;
    }

    private Long firstLong(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long l) {
                return l;
            }
        }
        return null;
    }
}
