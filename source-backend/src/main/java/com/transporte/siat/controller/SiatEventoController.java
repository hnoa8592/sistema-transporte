package com.transporte.siat.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.siat.dto.SiatEventoRequest;
import com.transporte.siat.dto.SiatEventoResponse;
import com.transporte.siat.service.SiatEventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/siat/eventos")
@RequiredArgsConstructor
@Tag(name = "SIAT - Eventos Significativos",
        description = "Registro de eventos significativos en el SIN Bolivia. " +
                "Los eventos son requeridos para justificar la emisión de facturas en modo offline.")
@SecurityRequirement(name = "Bearer Authentication")
public class SiatEventoController {

    private final SiatEventoService eventoService;

    @GetMapping("/{configId}")
    @Operation(summary = "Listar eventos por configuración SIAT")
    public ResponseEntity<ApiResponse<PageResponse<SiatEventoResponse>>> findByConfig(
            @PathVariable UUID configId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok(eventoService.findByConfig(configId, pageable)));
    }

    @PostMapping
    @Operation(
            summary = "Registrar evento significativo",
            description = "Registra un evento significativo en el SIN Bolivia. " +
                    "Códigos de evento: 1=Corte de energía, 2=Sin internet, 3=Falla del sistema, " +
                    "4=Condiciones climáticas, 5=Autorización previa SIN."
    )
    public ResponseEntity<ApiResponse<SiatEventoResponse>> registrar(
            @Valid @RequestBody SiatEventoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(eventoService.registrar(request)));
    }
}
