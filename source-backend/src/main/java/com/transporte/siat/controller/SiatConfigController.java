package com.transporte.siat.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.siat.dto.SiatConfigRequest;
import com.transporte.siat.dto.SiatConfigResponse;
import com.transporte.siat.service.SiatConfigService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/siat/config")
@RequiredArgsConstructor
@Tag(name = "SIAT - Configuración", description = "Configuración de credenciales SIAT por sucursal/punto de venta")
@SecurityRequirement(name = "Bearer Authentication")
public class SiatConfigController {

    private final SiatConfigService configService;

    @GetMapping
    @Operation(summary = "Listar configuraciones SIAT")
    public ResponseEntity<ApiResponse<List<SiatConfigResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(configService.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener configuración SIAT por ID")
    public ResponseEntity<ApiResponse<SiatConfigResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(configService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Crear configuración SIAT")
    public ResponseEntity<ApiResponse<SiatConfigResponse>> create(@Valid @RequestBody SiatConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(configService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar configuración SIAT")
    public ResponseEntity<ApiResponse<SiatConfigResponse>> update(@PathVariable UUID id,
                                                                   @Valid @RequestBody SiatConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(configService.update(id, request)));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Activar/desactivar configuración SIAT")
    public ResponseEntity<ApiResponse<Void>> toggle(@PathVariable UUID id) {
        configService.toggleActivo(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
