package com.transporte.encomiendas.dto;

import com.transporte.encomiendas.enums.ParcelStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ParcelTrackingResponse(
        UUID id,
        UUID parcelId,
        ParcelStatus status,
        String location,
        LocalDateTime timestamp,
        String notes
) {}
