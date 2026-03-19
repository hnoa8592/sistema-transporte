package com.transporte.pasajes.mapper;

import com.transporte.pasajes.dto.SeatMapResponse;
import com.transporte.pasajes.entity.SeatMap;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SeatMapMapper {
    SeatMapResponse toResponse(SeatMap seatMap);
}
