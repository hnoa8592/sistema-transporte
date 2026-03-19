package com.transporte.siat.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.siat.dto.SiatCatalogoResponse;
import com.transporte.siat.entity.SiatConfig;
import com.transporte.siat.enums.SiatTipoCatalogo;
import com.transporte.siat.service.SiatCatalogService;
import com.transporte.siat.service.SiatConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/siat/catalogos")
@RequiredArgsConstructor
@Tag(name = "SIAT - Catálogos", description = "Sincronización de catálogos SIN Bolivia")
@SecurityRequirement(name = "Bearer Authentication")
public class SiatCatalogController {

    private final SiatCatalogService catalogService;
    private final SiatConfigService configService;

    @GetMapping
    @Operation(
            summary = "Listar catálogo por tipo",
            description = "Devuelve los items de un catálogo sincronizado. Tipos disponibles: " +
                    "ACTIVIDAD_ECONOMICA, PRODUCTO_SERVICIO, LEYENDA_FACTURA, TIPO_DOCUMENTO_IDENTIDAD, " +
                    "METODO_PAGO, MONEDA, UNIDAD_MEDIDA"
    )
    public ResponseEntity<ApiResponse<List<SiatCatalogoResponse>>> findByTipo(
            @RequestParam SiatTipoCatalogo tipo) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findByTipo(tipo)));
    }

    @PostMapping("/sincronizar/{configId}")
    @Operation(
            summary = "Sincronizar todos los catálogos",
            description = "Descarga y actualiza todos los catálogos del SIN Bolivia: actividades económicas, " +
                    "productos/servicios, leyendas de factura, tipos de documento, métodos de pago, monedas y unidades de medida. " +
                    "Requiere CUIS vigente."
    )
    public ResponseEntity<ApiResponse<Map<String, Integer>>> sincronizarTodos(@PathVariable UUID configId) {
        SiatConfig config = configService.getById(configId);
        return ResponseEntity.ok(ApiResponse.ok(catalogService.sincronizarTodos(config)));
    }
}
