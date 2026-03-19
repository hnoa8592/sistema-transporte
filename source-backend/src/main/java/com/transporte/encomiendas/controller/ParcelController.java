package com.transporte.encomiendas.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.encomiendas.dto.*;
import com.transporte.encomiendas.enums.ParcelStatus;
import com.transporte.encomiendas.service.ParcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
@Tag(name = "Parcels", description = "Parcel management and tracking endpoints")
public class ParcelController {

    private final ParcelService parcelService;

    @GetMapping
    @Operation(
            summary = "Get all parcels paginated",
            description = "Returns a paginated list of all parcels regardless of status."
    )
    public ResponseEntity<ApiResponse<PageResponse<ParcelResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(parcelService.findAll(pageable)));
    }

    @GetMapping("/status/{status}")
    @Operation(
            summary = "Get parcels by status",
            description = "Returns a paginated list of parcels filtered by their current status " +
                    "(RECIBIDO, EN_TRANSITO, EN_DESTINO, ENTREGADO, DEVUELTO)."
    )
    public ResponseEntity<ApiResponse<PageResponse<ParcelResponse>>> findByStatus(
            @Parameter(description = "Parcel status to filter by", required = true)
            @PathVariable ParcelStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(parcelService.findByStatus(status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get parcel by ID",
            description = "Returns a single parcel by its UUID."
    )
    public ResponseEntity<ApiResponse<ParcelResponse>> findById(
            @Parameter(description = "Parcel UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(parcelService.findById(id)));
    }

    @GetMapping("/tracking/{trackingCode}")
    @Operation(
            summary = "Get parcel by tracking code",
            description = "Returns a parcel by its unique tracking code (e.g. PCL-XXXXXXXX)."
    )
    public ResponseEntity<ApiResponse<ParcelResponse>> findByTrackingCode(
            @Parameter(description = "Parcel tracking code", required = true)
            @PathVariable String trackingCode) {
        return ResponseEntity.ok(ApiResponse.ok(parcelService.findByTrackingCode(trackingCode)));
    }

    @GetMapping("/{id}/tracking")
    @Operation(
            summary = "Get tracking history for parcel by ID",
            description = "Returns the full tracking history for a parcel identified by its UUID, ordered by timestamp descending."
    )
    public ResponseEntity<ApiResponse<List<ParcelTrackingResponse>>> getTracking(
            @Parameter(description = "Parcel UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(parcelService.getTracking(id)));
    }

    @GetMapping("/tracking/{trackingCode}/history")
    @Operation(
            summary = "Get tracking history by tracking code (public endpoint)",
            description = "Returns the full tracking history for a parcel by its tracking code. " +
                    "This endpoint is intended for use by recipients to check parcel status without authentication."
    )
    public ResponseEntity<ApiResponse<List<ParcelTrackingResponse>>> getTrackingByCode(
            @Parameter(description = "Parcel tracking code", required = true)
            @PathVariable String trackingCode) {
        return ResponseEntity.ok(ApiResponse.ok(parcelService.getTrackingByCode(trackingCode)));
    }

    @PostMapping
    @Operation(
            summary = "Create a new parcel",
            description = "Registers a new parcel in the system. " +
                    "A unique tracking code is automatically generated. " +
                    "The initial status is set to RECIBIDO and a tracking record is created."
    )
    public ResponseEntity<ApiResponse<ParcelResponse>> create(
            @Valid @RequestBody ParcelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Parcel created", parcelService.create(request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update parcel status",
            description = "Updates the status of a parcel following the allowed state machine transitions: " +
                    "RECIBIDO -> EN_TRANSITO -> EN_DESTINO -> ENTREGADO or DEVUELTO. " +
                    "A new tracking record is automatically created for each status change."
    )
    public ResponseEntity<ApiResponse<ParcelResponse>> updateStatus(
            @Parameter(description = "Parcel UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UpdateParcelStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated", parcelService.updateStatus(id, request)));
    }
}
