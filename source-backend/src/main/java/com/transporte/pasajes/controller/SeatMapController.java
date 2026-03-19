package com.transporte.pasajes.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.pasajes.dto.SeatMapResponse;
import com.transporte.pasajes.service.SeatMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seat-maps")
@RequiredArgsConstructor
@Tag(name = "Seat Maps", description = "Seat map management and availability endpoints")
public class SeatMapController {

    private final SeatMapService seatMapService;

    @GetMapping("/{scheduleId}")
    @Operation(
            summary = "Get seat map for a schedule and travel date",
            description = "Returns the full seat map for a given schedule and travel date. " +
                    "If the seat map does not exist yet, it is generated automatically based on the bus configuration."
    )
    public ResponseEntity<ApiResponse<List<SeatMapResponse>>> getSeatMap(
            @Parameter(description = "Schedule ID", required = true)
            @PathVariable UUID scheduleId,
            @Parameter(description = "Travel date in ISO format (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate) {
        return ResponseEntity.ok(ApiResponse.ok(seatMapService.getSeatMap(scheduleId, travelDate)));
    }

    @GetMapping("/{scheduleId}/available-count")
    @Operation(
            summary = "Count available seats for a schedule and travel date",
            description = "Returns the number of seats with AVAILABLE status for the given schedule and travel date."
    )
    public ResponseEntity<ApiResponse<Map<String, Long>>> countAvailable(
            @Parameter(description = "Schedule ID", required = true)
            @PathVariable UUID scheduleId,
            @Parameter(description = "Travel date in ISO format (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate) {
        long count = seatMapService.countAvailableSeats(scheduleId, travelDate);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("availableSeats", count)));
    }
}
