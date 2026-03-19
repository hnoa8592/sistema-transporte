package com.transporte.auditoria.controller;

import com.transporte.auditoria.dto.AuditLogResponse;
import com.transporte.auditoria.service.AuditService;
import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit log operations")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Get audit logs", description = "Retrieve paginated audit logs with optional filters")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> findAll(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        PageResponse<AuditLogResponse> result;

        if (entityType != null) {
            result = auditService.findByEntityType(entityType, pageable);
        } else if (username != null) {
            result = auditService.findByUsername(username, pageable);
        } else if (from != null && to != null) {
            result = auditService.findByDateRange(from, to, pageable);
        } else {
            result = auditService.findAll(pageable);
        }

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
