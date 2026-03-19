package com.transporte.finanzas.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.finanzas.dto.CashTransactionRequest;
import com.transporte.finanzas.dto.CashTransactionResponse;
import com.transporte.finanzas.service.CashTransactionService;
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
@RequestMapping("/api/v1/cash-transactions")
@RequiredArgsConstructor
@Tag(name = "Cash Transactions", description = "Gestión de transacciones de caja")
@SecurityRequirement(name = "Bearer Authentication")
public class CashTransactionController {

    private final CashTransactionService cashTransactionService;

    @Operation(
            summary = "Listar transacciones de una caja",
            description = "Retorna una lista paginada de todas las transacciones de una caja registradora específica"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Lista de transacciones obtenida exitosamente",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    @GetMapping("/register/{cashRegisterId}")
    public ResponseEntity<ApiResponse<PageResponse<CashTransactionResponse>>> findByCashRegister(
            @Parameter(description = "ID de la caja registradora", required = true) @PathVariable UUID cashRegisterId,
            @Parameter(description = "Número de página (0-indexado)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de la página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Dirección de ordenamiento (asc/desc)") @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDirection, "createdAt"));
        PageResponse<CashTransactionResponse> response = cashTransactionService.findByCashRegister(cashRegisterId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "Registrar transacción de caja",
            description = "Registra un nuevo ingreso o egreso en una caja registradora abierta"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Transacción registrada exitosamente",
                    content = @Content(schema = @Schema(implementation = CashTransactionResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "La caja no existe, no está abierta, o datos inválidos"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CashTransactionResponse>> create(
            @Valid @RequestBody CashTransactionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(cashTransactionService.create(request)));
    }
}
