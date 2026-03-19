package com.transporte.operacion.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ScheduleRequest(
        @NotNull(message = "Route is required") UUID routeId,
        @NotNull(message = "Bus is required") UUID busId,
        @NotNull(message = "Driver is required") UUID driverId,
        @NotNull(message = "Departure time is required") LocalTime departureTime,
        LocalTime arrivalTime,
        List<Integer> daysOfWeek,
        boolean active,
        String notes
) {}
