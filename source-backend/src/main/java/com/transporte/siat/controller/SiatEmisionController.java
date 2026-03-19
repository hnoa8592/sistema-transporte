package com.transporte.siat.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.siat.dto.SiatEmisionRequest;
import com.transporte.siat.dto.SiatEmisionResponse;
import com.transporte.siat.dto.SiatPaqueteResponse;
import com.transporte.siat.service.SiatEmisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/siat/emision")
@RequiredArgsConstructor
@Tag(name = "SIAT - Emisión", description = "Emisión individual, en paquete y masiva de facturas SIAT Bolivia")
@SecurityRequirement(name = "Bearer Authentication")
public class SiatEmisionController {

    private final SiatEmisionService emisionService;

    // ======================== CONSULTAS ========================

    @GetMapping("/{id}")
    @Operation(summary = "Obtener factura SIAT por ID")
    public ResponseEntity<ApiResponse<SiatEmisionResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(emisionService.findById(id)));
    }

    @GetMapping("/invoice/{invoiceId}")
    @Operation(summary = "Obtener factura SIAT por ID de factura interna")
    public ResponseEntity<ApiResponse<SiatEmisionResponse>> findByInvoiceId(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(ApiResponse.ok(emisionService.findByInvoiceId(invoiceId)));
    }

    @GetMapping("/cuf/{cuf}")
    @Operation(summary = "Obtener factura SIAT por CUF")
    public ResponseEntity<ApiResponse<SiatEmisionResponse>> findByCuf(@PathVariable String cuf) {
        return ResponseEntity.ok(ApiResponse.ok(emisionService.findByCuf(cuf)));
    }

    // ======================== EMISIÓN INDIVIDUAL ========================

    @PostMapping("/individual")
    @Operation(
            summary = "Emitir factura individual",
            description = "Emite una factura de forma individual hacia el SIN Bolivia (modo online). " +
                    "El flujo completo es: CUIS → CUFD → generar CUF → construir XML → enviar al SIN. " +
                    "Para emisión offline, usar tipoEmision=2 y luego agrupar en paquete."
    )
    public ResponseEntity<ApiResponse<SiatEmisionResponse>> emitirIndividual(
            @Valid @RequestBody SiatEmisionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(emisionService.emitirFactura(request)));
    }

    @GetMapping("/{id}/estado")
    @Operation(
            summary = "Verificar estado de factura en el SIN",
            description = "Consulta el estado actual de una factura en el SIN Bolivia."
    )
    public ResponseEntity<ApiResponse<SiatEmisionResponse>> verificarEstado(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(emisionService.verificarEstado(id)));
    }

    // ======================== PAQUETE ========================

    @PostMapping("/paquete/{configId}")
    @Operation(
            summary = "Emitir paquete de facturas",
            description = "Agrupa todas las facturas con estado EN_PAQUETE para la configuración indicada " +
                    "en un ZIP y lo envía al SIN. Usado para emisión offline (fuera de línea). " +
                    "Requiere haber registrado un Evento Significativo previamente."
    )
    public ResponseEntity<ApiResponse<SiatPaqueteResponse>> emitirPaquete(
            @Parameter(description = "ID de la configuración SIAT") @PathVariable UUID configId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(emisionService.emitirPaquete(configId)));
    }

    @PostMapping("/paquete/{paqueteId}/validar")
    @Operation(
            summary = "Validar estado de recepción del paquete",
            description = "Consulta al SIN el resultado de validación de un paquete previamente enviado."
    )
    public ResponseEntity<ApiResponse<SiatPaqueteResponse>> validarPaquete(@PathVariable UUID paqueteId) {
        return ResponseEntity.ok(ApiResponse.ok(emisionService.validarPaquete(paqueteId)));
    }

    // ======================== MASIVA ========================

    @PostMapping("/masiva/{configId}")
    @Operation(
            summary = "Emitir facturas en modo masivo",
            description = "Envía en lote todas las facturas con estado PENDIENTE para la configuración indicada."
    )
    public ResponseEntity<ApiResponse<SiatPaqueteResponse>> emitirMasiva(@PathVariable UUID configId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(emisionService.emitirMasiva(configId)));
    }

    @PostMapping("/masiva/{paqueteId}/validar")
    @Operation(summary = "Validar resultado de emisión masiva")
    public ResponseEntity<ApiResponse<SiatPaqueteResponse>> validarMasiva(@PathVariable UUID paqueteId) {
        return ResponseEntity.ok(ApiResponse.ok(emisionService.validarMasiva(paqueteId)));
    }
}
