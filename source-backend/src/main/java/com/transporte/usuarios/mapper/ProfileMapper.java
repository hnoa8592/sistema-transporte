package com.transporte.usuarios.mapper;

import com.transporte.usuarios.dto.ProfileRequest;
import com.transporte.usuarios.dto.ProfileResponse;
import com.transporte.usuarios.entity.Profile;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {ResourceMapper.class})
public interface ProfileMapper {
    @Mapping(target = "resources", ignore = true)
    Profile toEntity(ProfileRequest request);

    ProfileResponse toResponse(Profile profile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "resources", ignore = true)
    void updateFromRequest(ProfileRequest request, @MappingTarget Profile profile);
}
