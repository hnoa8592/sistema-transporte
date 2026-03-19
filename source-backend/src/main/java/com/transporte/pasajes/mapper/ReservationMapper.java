package com.transporte.pasajes.mapper;

import com.transporte.pasajes.dto.ReservationResponse;
import com.transporte.pasajes.entity.Reservation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    ReservationResponse toResponse(Reservation reservation);
}
