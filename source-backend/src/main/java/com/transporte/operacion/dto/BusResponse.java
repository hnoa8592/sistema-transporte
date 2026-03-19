package com.transporte.operacion.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BusResponse(
        UUID id,
        String plate,
        String model,
        String brand,
        Integer year,
        UUID fleetId,
        String fleetName,
        boolean hasTwoFloors,
        int totalSeats,
        Integer seatsFirstFloor,
        Integer seatsSecondFloor,
        boolean active,
        String notes,
        LocalDateTime createdAt
) {}
