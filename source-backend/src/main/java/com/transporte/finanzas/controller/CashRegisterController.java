package com.transporte.finanzas.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.finanzas.dto.*;
import com.transporte.finanzas.service.CashRegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/v1/cash-registers")
@RequiredArgsConstructor
@Tag(name = "Cash Registers", description = "Gestión de cajas registradoras")
@SecurityRequirement(name = "Bearer Authentication")
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    @Operation(
            summary = "Listar cajas registradoras",
            description = "Retorna una lista paginada de todas las cajas registradoras"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Lista de cajas registradoras obtenida exitosamente",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CashRegisterResponse>>> findAll(
            @Parameter(description = "Número de página (0-indexado)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de la página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "openedAt") String sort,
            @Parameter(description = "Dirección de ordenamiento (asc/desc)") @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        PageResponse<CashRegisterResponse> response = cashRegisterService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Caja registradora encontrada exitosamente",
                    content = @Content(schema = @Schema(implementation = CashRegisterResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Caja registradora no encontrada"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    @GetMapping("/employee/{employeeId}/open")
    @Operation(
            summary = "Obtener caja abierta del empleado",
            description = "Retorna la caja registradora actualmente abierta para el empleado indicado, o data=null si no tiene caja abierta."
    )
    public ResponseEntity<ApiResponse<CashRegisterResponse>> findOpenByEmployee(
            @Parameter(description = "ID del empleado", required = true) @PathVariable UUID employeeId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                cashRegisterService.findOpenByEmployee(employeeId).orElse(null)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CashRegisterResponse>> findById(
            @Parameter(description = "ID de la caja registradora", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cashRegisterService.findById(id)));
    }

    @Operation(
            summary = "Obtener resumen de caja",
            description = "Retorna el resumen financiero de una caja registradora incluyendo totales de ingresos y egresos"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Resumen de caja obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = CashRegisterSummaryResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Caja registradora no encontrada"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    @GetMapping("/{id}/summary")
    public ResponseEntity<ApiResponse<CashRegisterSummaryResponse>> getSummary(
            @Parameter(description = "ID de la caja registradora", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cashRegisterService.getSummary(id)));
    }

    @Operation(
            summary = "Abrir caja registradora",
            description = "Abre una nueva caja registradora para un empleado. Solo puede existir una caja abierta por empleado."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Caja registradora abierta exitosamente",
                    content = @Content(schema = @Schema(implementation = CashRegisterResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "El empleado ya tiene una caja abierta o datos inválidos"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    @PostMapping("/open")
    public ResponseEntity<ApiResponse<CashRegisterResponse>> open(
            @Valid @RequestBody CashRegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(cashRegisterService.open(request)));
    }

    @Operation(
            summary = "Cerrar caja registradora",
            description = "Cierra una caja registradora abierta registrando el monto final"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Caja registradora cerrada exitosamente",
                    content = @Content(schema = @Schema(implementation = CashRegisterResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "La caja ya está cerrada o datos inválidos"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Caja registradora no encontrada"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<CashRegisterResponse>> close(
            @Parameter(description = "ID de la caja registradora", required = true) @PathVariable UUID id,
            @Valid @RequestBody CloseCashRegisterRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cashRegisterService.close(id, request)));
    }
}
