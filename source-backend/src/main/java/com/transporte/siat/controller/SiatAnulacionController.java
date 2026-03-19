package com.transporte.siat.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.siat.dto.SiatAnulacionRequest;
import com.transporte.siat.dto.SiatEmisionResponse;
import com.transporte.siat.dto.SiatReversionRequest;
import com.transporte.siat.service.SiatAnulacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/siat/anulacion")
@RequiredArgsConstructor
@Tag(name = "SIAT - Anulación", description = "Anulación y reversión de facturas en el SIN Bolivia")
@SecurityRequirement(name = "Bearer Authentication")
public class SiatAnulacionController {

    private final SiatAnulacionService anulacionService;

    @PostMapping
    @Operation(
            summary = "Anular factura en el SIN",
            description = "Anula una factura previamente emitida y válida en el SIN Bolivia. " +
                    "Se debe indicar el código de motivo de anulación del catálogo SIAT."
    )
    public ResponseEntity<ApiResponse<SiatEmisionResponse>> anular(
            @Valid @RequestBody SiatAnulacionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(anulacionService.anular(request)));
    }

    @PostMapping("/revertir")
    @Operation(
            summary = "Revertir anulación de factura",
            description = "Revierte la anulación de una factura que fue anulada previamente en el SIN Bolivia. " +
                    "La factura vuelve a estado activo/válido."
    )
    public ResponseEntity<ApiResponse<SiatEmisionResponse>> revertir(
            @Valid @RequestBody SiatReversionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(anulacionService.revertir(request)));
    }
}
