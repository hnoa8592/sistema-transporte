package com.transporte.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResourceRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "HTTP method is required")
        @Pattern(regexp = "GET|POST|PUT|DELETE|PATCH", message = "Invalid HTTP method") String httpMethod,
        @NotBlank(message = "Endpoint is required") String endpoint,
        String module,
        String description,
        boolean active
) {}
