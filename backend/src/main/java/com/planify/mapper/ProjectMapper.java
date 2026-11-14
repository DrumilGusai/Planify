package com.planify.mapper;

import com.planify.dto.ProjectDtos.ProjectDto;
import com.planify.entity.Project;
import com.planify.entity.ProjectRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "myRole", source = "myRole")
    ProjectDto toDto(Project project, ProjectRole myRole);
}
