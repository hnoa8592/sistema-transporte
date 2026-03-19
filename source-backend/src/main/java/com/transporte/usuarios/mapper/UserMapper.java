package com.transporte.usuarios.mapper;

import com.transporte.usuarios.dto.UpdateUserRequest;
import com.transporte.usuarios.dto.UserRequest;
import com.transporte.usuarios.dto.UserResponse;
import com.transporte.usuarios.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "profile", ignore = true)
    User toEntity(UserRequest request);

    @Mapping(target = "profileId", source = "profile.id")
    @Mapping(target = "profileName", source = "profile.name")
    UserResponse toResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateFromRequest(UpdateUserRequest request, @MappingTarget User user);
}
