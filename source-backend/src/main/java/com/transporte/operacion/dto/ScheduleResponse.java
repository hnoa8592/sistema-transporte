package com.transporte.operacion.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ScheduleResponse(
        UUID id,
        UUID routeId,
        String routeDescription,
        UUID busId,
        String busPlate,
        UUID driverId,
        String driverName,
        LocalTime departureTime,
        LocalTime arrivalTime,
        List<Integer> daysOfWeek,
        boolean active,
        String notes,
        LocalDateTime createdAt
) {}
