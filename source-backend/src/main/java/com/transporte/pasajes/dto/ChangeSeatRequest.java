package com.transporte.pasajes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChangeSeatRequest(
        @NotNull @Positive int newSeatNumber,
        @NotNull int newFloorNumber
) {}
