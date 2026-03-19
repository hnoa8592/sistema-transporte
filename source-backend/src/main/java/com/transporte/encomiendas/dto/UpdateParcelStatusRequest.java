package com.transporte.encomiendas.dto;

import com.transporte.encomiendas.enums.ParcelStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateParcelStatusRequest(
        @NotNull(message = "Status is required") ParcelStatus status,
        String location,
        String notes
) {}
