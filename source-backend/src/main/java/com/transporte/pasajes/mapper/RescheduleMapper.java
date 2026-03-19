package com.transporte.pasajes.mapper;

import com.transporte.pasajes.dto.RescheduleResponse;
import com.transporte.pasajes.entity.Reschedule;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RescheduleMapper {
    RescheduleResponse toResponse(Reschedule reschedule);
}
