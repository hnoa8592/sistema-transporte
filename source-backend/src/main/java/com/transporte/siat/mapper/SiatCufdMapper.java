package com.transporte.siat.mapper;

import com.transporte.siat.dto.SiatCufdResponse;
import com.transporte.siat.entity.SiatCufd;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SiatCufdMapper {

    @Mapping(target = "vigente", expression = "java(entity.isVigente())")
    SiatCufdResponse toResponse(SiatCufd entity);
}
