package com.transporte.finanzas.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.finanzas.dto.SystemParameterRequest;
import com.transporte.finanzas.dto.SystemParameterResponse;
import com.transporte.finanzas.service.SystemParameterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parameters")
@RequiredArgsConstructor
@Tag(name = "System Parameters", description = "Gestión de parámetros del sistema")
@SecurityRequirement(name = "Bearer Authentication")
public class SystemParameterController {

    private final SystemParameterService systemParameterService;

    @Operation(
            summary = "Listar parámetros del sistema",
            description = "Retorna todos los parámetros activos del sistema. Los resultados se cachean para optimizar el rendimiento."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Lista de parámetros obtenida exitosamente",
                    content = @Content(schema = @Schema(implementation = SystemParameterResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemParameterResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(systemParameterService.findAll()));
    }

    @Operation(
            summary = "Obtener parámetro por ID",
            description = "Retorna los detalles de un parámetro del sistema específico"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Parámetro encontrado exitosamente",
                    content = @Content(schema = @Schema(implementation = SystemParameterResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Parámetro no encontrado"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemParameterResponse>> findById(
            @Parameter(description = "ID del parámetro", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(systemParameterService.findById(id)));
    }

    @Operation(
            summary = "Crear parámetro del sistema",
            description = "Crea un nuevo parámetro de configuración del sistema. La clave debe ser única."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Parámetro creado exitosamente",
                    content = @Content(schema = @Schema(implementation = SystemParameterResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "La clave ya existe o datos inválidos"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SystemParameterResponse>> create(
            @Valid @RequestBody SystemParameterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(systemParameterService.create(request)));
    }

    @Operation(
            summary = "Actualizar parámetro del sistema",
            description = "Actualiza un parámetro de configuración del sistema existente. Invalida el caché al actualizar."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Parámetro actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = SystemParameterResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Parámetro no encontrado"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemParameterResponse>> update(
            @Parameter(description = "ID del parámetro", required = true) @PathVariable UUID id,
            @Valid @RequestBody SystemParameterRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(systemParameterService.update(id, request)));
    }
}
