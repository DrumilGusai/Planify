package com.planify.mapper;

import com.planify.dto.WorkspaceDtos.WorkspaceDto;
import com.planify.entity.Workspace;
import com.planify.entity.WorkspaceRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkspaceMapper {

    @Mapping(target = "myRole", source = "myRole")
    WorkspaceDto toDto(Workspace workspace, WorkspaceRole myRole);
}
