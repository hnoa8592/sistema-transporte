package com.transporte.pasajes.mapper;

import com.transporte.pasajes.dto.TicketRequest;
import com.transporte.pasajes.dto.TicketResponse;
import com.transporte.pasajes.entity.Ticket;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TicketMapper {
    Ticket toEntity(TicketRequest request);
    TicketResponse toResponse(Ticket ticket);
}
