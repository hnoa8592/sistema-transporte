package com.transporte.siat.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.siat.dto.SiatCufdResponse;
import com.transporte.siat.dto.SiatCuisResponse;
import com.transporte.siat.entity.SiatConfig;
import com.transporte.siat.service.SiatCodesService;
import com.transporte.siat.service.SiatConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/siat/codigos")
@RequiredArgsConstructor
@Tag(name = "SIAT - Códigos", description = "Obtención de CUIS y CUFD del SIN Bolivia")
@SecurityRequirement(name = "Bearer Authentication")
public class SiatCodesController {

    private final SiatCodesService codesService;
    private final SiatConfigService configService;

    // ========================== CUIS ==========================

    @PostMapping("/cuis/{configId}")
    @Operation(
            summary = "Obtener CUIS del SIN",
            description = "Solicita un nuevo Código Único de Inicio de Sistema (CUIS) al SIN Bolivia. " +
                    "El CUIS tiene vigencia de 6 meses y es prerrequisito para obtener el CUFD."
    )
    public ResponseEntity<ApiResponse<SiatCuisResponse>> obtenerCuis(
            @Parameter(description = "ID de la configuración SIAT") @PathVariable UUID configId) {
        SiatConfig config = configService.getById(configId);
        return ResponseEntity.ok(ApiResponse.ok(codesService.obtenerCuis(config)));
    }

    @GetMapping("/cuis/{configId}/vigente")
    @Operation(
            summary = "Consultar CUIS vigente",
            description = "Devuelve el CUIS actualmente vigente para la configuración indicada."
    )
    public ResponseEntity<ApiResponse<String>> getCuisVigente(@PathVariable UUID configId) {
        SiatConfig config = configService.getById(configId);
        String cuis = codesService.getCuisVigente(
                config.getId(), config.getCodigoSucursal(), config.getCodigoPuntoVenta()).getCuis();
        return ResponseEntity.ok(ApiResponse.ok(cuis));
    }

    // ========================== CUFD ==========================

    @PostMapping("/cufd/{configId}")
    @Operation(
            summary = "Obtener CUFD del SIN",
            description = "Solicita un nuevo Código Único de Facturación Diaria (CUFD) al SIN Bolivia. " +
                    "El CUFD tiene vigencia de 24 horas y es requerido para cada jornada de facturación. " +
                    "Requiere que exista un CUIS vigente."
    )
    public ResponseEntity<ApiResponse<SiatCufdResponse>> obtenerCufd(
            @Parameter(description = "ID de la configuración SIAT") @PathVariable UUID configId) {
        SiatConfig config = configService.getById(configId);
        return ResponseEntity.ok(ApiResponse.ok(codesService.obtenerCufd(config)));
    }

    @GetMapping("/cufd/{configId}/vigente")
    @Operation(
            summary = "Consultar CUFD vigente",
            description = "Devuelve el CUFD actualmente vigente para la configuración indicada."
    )
    public ResponseEntity<ApiResponse<SiatCufdResponse>> getCufdVigente(@PathVariable UUID configId) {
        SiatConfig config = configService.getById(configId);
        var cufd = codesService.getCufdVigente(
                config.getId(), config.getCodigoSucursal(), config.getCodigoPuntoVenta());
        return ResponseEntity.ok(ApiResponse.ok(
                new SiatCufdResponse(cufd.getId(), cufd.getCufd(), cufd.getCodigoControl(),
                        cufd.getCodigoParaQr(), cufd.getFechaVigencia(),
                        cufd.getCodigoSucursal(), cufd.getCodigoPuntoVenta(),
                        cufd.getActivo(), cufd.isVigente(), cufd.getCreatedAt())));
    }
}
