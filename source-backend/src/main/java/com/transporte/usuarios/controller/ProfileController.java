package com.transporte.usuarios.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.usuarios.dto.ProfileRequest;
import com.transporte.usuarios.dto.ProfileResponse;
import com.transporte.usuarios.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "Profile management endpoints")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Get all profiles paginated")
    public ResponseEntity<ApiResponse<PageResponse<ProfileResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get profile by ID with resources")
    public ResponseEntity<ApiResponse<ProfileResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> create(@Valid @RequestBody ProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Profile created", profileService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", profileService.update(id, request)));
    }

    @PostMapping("/{id}/resources")
    @Operation(summary = "Assign resources to profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> assignResources(
            @PathVariable UUID id, @RequestBody Set<UUID> resourceIds) {
        return ResponseEntity.ok(ApiResponse.ok("Resources assigned", profileService.assignResources(id, resourceIds)));
    }

    @DeleteMapping("/{id}/resources")
    @Operation(summary = "Remove resources from profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> removeResources(
            @PathVariable UUID id, @RequestBody Set<UUID> resourceIds) {
        return ResponseEntity.ok(ApiResponse.ok("Resources removed", profileService.removeResources(id, resourceIds)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a profile")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        profileService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
