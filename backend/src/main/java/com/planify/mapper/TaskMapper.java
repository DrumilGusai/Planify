package com.planify.mapper;

import com.planify.dto.TaskDtos.CreateTaskReq;
import com.planify.dto.TaskDtos.TaskDto;
import com.planify.dto.TaskDtos.UpdateTaskReq;
import com.planify.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "projectId", ignore = true)
    @Mapping(target = "status", constant = "TODO")
    @Mapping(target = "priority", expression = "java(req.priority() != null ? req.priority() : com.planify.entity.TaskPriority.MEDIUM)")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    Task toEntity(CreateTaskReq req);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "projectId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void update(UpdateTaskReq req, @MappingTarget Task target);

    @Mapping(target = "workspaceId", source = "workspaceId")
    @Mapping(target = "assigneeName", source = "assigneeName")
    TaskDto toDto(Task task, Long workspaceId, String assigneeName);
}
