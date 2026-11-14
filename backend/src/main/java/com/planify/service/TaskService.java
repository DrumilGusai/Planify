package com.planify.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.planify.audit.Audited;
import com.planify.cache.SafeCache;
import com.planify.dto.TaskDtos.CreateTaskReq;
import com.planify.dto.TaskDtos.PageRes;
import com.planify.dto.TaskDtos.TaskDto;
import com.planify.dto.TaskDtos.TaskFilter;
import com.planify.dto.TaskDtos.UpdateTaskReq;
import com.planify.entity.PlatformRole;
import com.planify.entity.Project;
import com.planify.entity.ProjectStatus;
import com.planify.entity.Task;
import com.planify.entity.TaskStatus;
import com.planify.entity.User;
import com.planify.exception.BadRequestException;
import com.planify.exception.NotFoundException;
import com.planify.mapper.TaskMapper;
import com.planify.repository.ProjectMemberRepository;
import com.planify.repository.ProjectRepository;
import com.planify.repository.TaskRepository;
import com.planify.repository.UserRepository;
import com.planify.security.AuthPrincipal;
import com.planify.specification.TaskSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "dueDate", "priority", "status", "title"
    );

    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final ProjectMemberRepository projectMembers;
    private final UserRepository users;
    private final TaskMapper mapper;
    private final SafeCache cache;

    @Transactional(readOnly = true)
    public PageRes<TaskDto> list(Long pid, TaskFilter filter, Pageable pageable) {
        Project project = loadProject(pid);
        Pageable clamped = clampAndValidate(pageable);

        long version = cache.version(pid);
        String key = "tasks:p%d:v%d:%s".formatted(pid, version, hashParams(filter, clamped));

        return cache.get(key, new TypeReference<PageRes<TaskDto>>() {}, 5, () -> {
            List<Long> ftIds = resolveSearchIds(pid, filter != null ? filter.q() : null);
            Page<Task> page = tasks.findAll(TaskSpecs.ofProject(pid, filter, ftIds), clamped);
            return toPageRes(page, Map.of(pid, project.getWorkspaceId()));
        });
    }

    @Transactional(readOnly = true)
    public PageRes<TaskDto> myTasks(AuthPrincipal principal, TaskFilter filter, Pageable pageable) {
        Pageable clamped = clampAndValidate(pageable);
        List<Long> accessibleProjectIds = principal.role() == PlatformRole.SUPER_ADMIN
                ? projects.findAll().stream().map(Project::getId).toList()
                : projects.findAccessibleProjectIds(principal.userId());

        Page<Task> page = tasks.findAll(
                TaskSpecs.forAssignee(principal.userId(), accessibleProjectIds, filter),
                clamped
        );
        Map<Long, Long> projectWorkspaceMap = projects.findAllById(
                        page.getContent().stream().map(Task::getProjectId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Project::getId, Project::getWorkspaceId));

        return toPageRes(page, projectWorkspaceMap);
    }

    @Transactional
    @Audited(entity = "TASK", action = "CREATE")
    public TaskDto create(Long pid, CreateTaskReq req, Long userId) {
        Project project = loadProject(pid);
        ensureNotArchived(project);
        validateAssignee(pid, req.assigneeId());

        Task task = mapper.toEntity(req);
        task.setProjectId(pid);
        task.setCreatedBy(userId);
        Task saved = tasks.save(task);

        afterCommit(() -> cache.bump(pid));
        return mapper.toDto(saved, project.getWorkspaceId(), resolveUserName(saved.getAssigneeId()));
    }

    @Transactional(readOnly = true)
    public TaskDto get(Long id) {
        return cache.get("task:" + id, TaskDto.class, 10, () -> {
            Task task = loadTask(id);
            Project project = loadProject(task.getProjectId());
            return mapper.toDto(task, project.getWorkspaceId(), resolveUserName(task.getAssigneeId()));
        });
    }

    @Transactional
    @Audited(entity = "TASK", action = "UPDATE")
    public TaskDto update(Long id, UpdateTaskReq req) {
        Task task = loadTask(id);
        Project project = loadProject(task.getProjectId());
        ensureNotArchived(project);
        validateAssignee(task.getProjectId(), req.assigneeId());

        mapper.update(req, task);
        Task saved = tasks.save(task);

        afterCommit(() -> {
            cache.bump(saved.getProjectId());
            cache.evict("task:" + id);
        });
        return mapper.toDto(saved, project.getWorkspaceId(), resolveUserName(saved.getAssigneeId()));
    }

    @Transactional
    @Audited(entity = "TASK", action = "UPDATE")
    public TaskDto changeStatus(Long id, TaskStatus status) {
        Task task = loadTask(id);
        Project project = loadProject(task.getProjectId());
        ensureNotArchived(project);

        task.setStatus(status);
        Task saved = tasks.save(task);

        afterCommit(() -> {
            cache.bump(saved.getProjectId());
            cache.evict("task:" + id);
        });
        return mapper.toDto(saved, project.getWorkspaceId(), resolveUserName(saved.getAssigneeId()));
    }

    @Transactional
    @Audited(entity = "TASK", action = "DELETE")
    public void delete(Long id) {
        Task task = loadTask(id);
        Project project = loadProject(task.getProjectId());
        ensureNotArchived(project);

        task.setDeleted(true);
        tasks.save(task);

        afterCommit(() -> {
            cache.bump(task.getProjectId());
            cache.evict("task:" + id);
        });
    }

    private PageRes<TaskDto> toPageRes(Page<Task> page, Map<Long, Long> projectWsMap) {
        Set<Long> assigneeIds = page.getContent().stream()
                .map(Task::getAssigneeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> names = assigneeIds.isEmpty()
                ? Map.of()
                : users.findAllById(assigneeIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        List<TaskDto> content = page.getContent().stream()
                .map(t -> mapper.toDto(
                        t,
                        projectWsMap.get(t.getProjectId()),
                        t.getAssigneeId() != null ? names.get(t.getAssigneeId()) : null
                ))
                .toList();

        return new PageRes<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private List<Long> resolveSearchIds(Long pid, String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String sanitized = q.replaceAll("[+\\-<>()~*\"@]", " ").trim();
        if (sanitized.isBlank()) {
            return List.of();
        }
        String[] tokens = sanitized.split("\\s+");
        boolean hasLongToken = Arrays.stream(tokens).anyMatch(t -> t.length() >= 3);
        if (!hasLongToken) {
            return tasks.searchIdsLike(pid, sanitized);
        }
        String booleanQuery = Arrays.stream(tokens)
                .filter(t -> !t.isBlank())
                .map(t -> "+" + t + "*")
                .collect(Collectors.joining(" "));
        return tasks.searchIds(pid, booleanQuery);
    }

    private Pageable clampAndValidate(Pageable pg) {
        int size = Math.min(Math.max(pg.getPageSize(), 1), MAX_PAGE_SIZE);
        Sort sort = pg.getSort().isSorted() ? pg.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Unsupported sort field: " + order.getProperty());
            }
        }
        return PageRequest.of(Math.max(pg.getPageNumber(), 0), size, sort);
    }

    private String hashParams(TaskFilter f, Pageable pg) {
        String raw = "%s|%s|%s|%s|%s|%s|%d|%d|%s".formatted(
                f != null ? f.status() : null,
                f != null ? f.priority() : null,
                f != null ? f.assigneeId() : null,
                f != null ? f.dueFrom() : null,
                f != null ? f.dueTo() : null,
                f != null ? f.q() : null,
                pg.getPageNumber(),
                pg.getPageSize(),
                pg.getSort()
        );
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(raw.hashCode());
        }
    }

    private void validateAssignee(Long pid, Long assigneeId) {
        if (assigneeId != null && !projectMembers.existsByProjectIdAndUserId(pid, assigneeId)) {
            throw new BadRequestException("Assignee must be a member of the project");
        }
    }

    private void ensureNotArchived(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new BadRequestException("Cannot modify tasks in an archived project");
        }
    }

    private Project loadProject(Long pid) {
        return projects.findById(pid)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    private Task loadTask(Long id) {
        return tasks.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }

    private String resolveUserName(Long userId) {
        if (userId == null) return null;
        return users.findById(userId).map(User::getName).orElse(null);
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
