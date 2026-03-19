package com.transporte.operacion.dto;

import jakarta.validation.constraints.Email;

public record CustomerRequest(
        String documentNumber,
        String documentType,
        String firstName,
        String lastName,
        @Email String email,
        String phone,
        String address,
        boolean active
) {}
