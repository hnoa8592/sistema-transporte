package com.transporte.operacion.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String documentNumber,
        String documentType,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        boolean active,
        LocalDateTime createdAt
) {}
