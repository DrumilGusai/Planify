package com.planify.service;

import com.planify.cache.SafeCache;
import com.planify.dto.TaskDtos.CreateTaskReq;
import com.planify.dto.TaskDtos.TaskDto;
import com.planify.dto.TaskDtos.TaskFilter;
import com.planify.entity.Project;
import com.planify.entity.ProjectStatus;
import com.planify.entity.Task;
import com.planify.entity.TaskPriority;
import com.planify.entity.TaskStatus;
import com.planify.exception.BadRequestException;
import com.planify.mapper.TaskMapper;
import com.planify.repository.ProjectMemberRepository;
import com.planify.repository.ProjectRepository;
import com.planify.repository.TaskRepository;
import com.planify.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository tasks;
    @Mock private ProjectRepository projects;
    @Mock private ProjectMemberRepository projectMembers;
    @Mock private UserRepository users;
    @Mock private TaskMapper mapper;
    @Mock private SafeCache cache;

    @InjectMocks
    private TaskService taskService;

    @Test
    void create_rejectsNonProjectMemberAssignee() {
        Project p = new Project();
        p.setId(1L);
        p.setWorkspaceId(10L);
        p.setStatus(ProjectStatus.ACTIVE);
        when(projects.findById(1L)).thenReturn(Optional.of(p));
        when(projectMembers.existsByProjectIdAndUserId(1L, 99L)).thenReturn(false);

        CreateTaskReq req = new CreateTaskReq("Task A", "Desc", TaskPriority.HIGH, 99L, null);

        assertThatThrownBy(() -> taskService.create(1L, req, 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Assignee");
    }

    @Test
    void create_rejectsArchivedProject() {
        Project p = new Project();
        p.setId(1L);
        p.setStatus(ProjectStatus.ARCHIVED);
        when(projects.findById(1L)).thenReturn(Optional.of(p));

        CreateTaskReq req = new CreateTaskReq("Task A", "Desc", TaskPriority.HIGH, null, null);

        assertThatThrownBy(() -> taskService.create(1L, req, 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void create_savesAndBumpsCache() {
        Project p = new Project();
        p.setId(1L);
        p.setWorkspaceId(10L);
        p.setStatus(ProjectStatus.ACTIVE);
        when(projects.findById(1L)).thenReturn(Optional.of(p));

        CreateTaskReq req = new CreateTaskReq("Task A", "Desc", TaskPriority.HIGH, null, null);
        Task entity = new Task();
        entity.setId(77L);
        entity.setProjectId(1L);
        entity.setTitle("Task A");

        when(mapper.toEntity(req)).thenReturn(entity);
        when(tasks.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity, 10L, null)).thenReturn(new TaskDto(
                77L, 10L, 1L, "Task A", "Desc", TaskStatus.TODO, TaskPriority.HIGH,
                null, null, null, 5L, Instant.now(), Instant.now()
        ));

        TaskDto dto = taskService.create(1L, req, 5L);

        assertThat(dto.id()).isEqualTo(77L);
        verify(cache).bump(1L);
    }

    @Test
    void list_rejectsUnsupportedSortProperty() {
        Project p = new Project();
        p.setId(1L);
        p.setWorkspaceId(10L);
        when(projects.findById(1L)).thenReturn(Optional.of(p));

        var pg = PageRequest.of(0, 20, Sort.by("passwordHash"));
        assertThatThrownBy(() -> taskService.list(1L, new TaskFilter(null, null, null, null, null, null), pg))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported sort field");
    }
}
