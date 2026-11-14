package com.planify.mapper;

import com.planify.dto.AuthDtos.UserDto;
import com.planify.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
