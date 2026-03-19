package com.transporte.pasajes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkTicketRequest(
        @NotEmpty(message = "At least one ticket is required")
        List<@Valid TicketRequest> tickets
) {}
