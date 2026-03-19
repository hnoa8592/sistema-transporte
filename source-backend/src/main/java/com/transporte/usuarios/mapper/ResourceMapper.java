package com.transporte.usuarios.mapper;

import com.transporte.usuarios.dto.ResourceRequest;
import com.transporte.usuarios.dto.ResourceResponse;
import com.transporte.usuarios.entity.Resource;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ResourceMapper {
    Resource toEntity(ResourceRequest request);
    ResourceResponse toResponse(Resource resource);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(ResourceRequest request, @MappingTarget Resource resource);
}
