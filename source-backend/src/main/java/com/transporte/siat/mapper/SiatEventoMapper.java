package com.transporte.siat.mapper;

import com.transporte.siat.dto.SiatEventoResponse;
import com.transporte.siat.entity.SiatEvento;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SiatEventoMapper {
    SiatEventoResponse toResponse(SiatEvento entity);
}
