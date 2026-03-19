package com.transporte.operacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record BusRequest(
        @NotBlank(message = "Plate is required") String plate,
        String model,
        String brand,
        Integer year,
        UUID fleetId,
        boolean hasTwoFloors,
        @NotNull @Positive int totalSeats,
        Integer seatsFirstFloor,
        Integer seatsSecondFloor,
        boolean active,
        String notes
) {}
