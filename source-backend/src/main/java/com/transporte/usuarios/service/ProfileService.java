package com.transporte.usuarios.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.usuarios.dto.ProfileRequest;
import com.transporte.usuarios.dto.ProfileResponse;
import com.transporte.usuarios.entity.Profile;
import com.transporte.usuarios.entity.Resource;
import com.transporte.usuarios.mapper.ProfileMapper;
import com.transporte.usuarios.repository.ProfileRepository;
import com.transporte.usuarios.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ResourceRepository resourceRepository;
    private final ProfileMapper profileMapper;

    public PageResponse<ProfileResponse> findAll(Pageable pageable) {
        return PageResponse.of(profileRepository.findAllByActiveTrue(pageable).map(profileMapper::toResponse));
    }

    @Cacheable(value = "profiles", key = "#id")
    public ProfileResponse findById(UUID id) {
        return profileMapper.toResponse(
                profileRepository.findByIdWithResources(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Profile", id))
        );
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Profile", description = "Creación de nuevo perfil de usuario")
    @Transactional
    @CacheEvict(value = "profiles", allEntries = true)
    public ProfileResponse create(ProfileRequest request) {
        if (profileRepository.existsByName(request.name())) {
            throw new BusinessException("Profile with name '" + request.name() + "' already exists");
        }
        Profile profile = profileMapper.toEntity(request);
        if (request.resourceIds() != null && !request.resourceIds().isEmpty()) {
            Set<Resource> resources = new HashSet<>(resourceRepository.findAllById(request.resourceIds()));
            profile.setResources(resources);
        }
        return profileMapper.toResponse(profileRepository.save(profile));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Profile", description = "Actualización de perfil de usuario")
    @Transactional
    @CacheEvict(value = "profiles", key = "#id")
    public ProfileResponse update(UUID id, ProfileRequest request) {
        Profile profile = profileRepository.findByIdWithResources(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", id));
        profileMapper.updateFromRequest(request, profile);
        if (request.resourceIds() != null) {
            Set<Resource> resources = new HashSet<>(resourceRepository.findAllById(request.resourceIds()));
            profile.setResources(resources);
        }
        return profileMapper.toResponse(profileRepository.save(profile));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Profile", description = "Asignación de recursos al perfil")
    @Transactional
    @CacheEvict(value = "profiles", key = "#profileId")
    public ProfileResponse assignResources(UUID profileId, Set<UUID> resourceIds) {
        Profile profile = profileRepository.findByIdWithResources(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", profileId));
        Set<Resource> resources = new HashSet<>(resourceRepository.findAllById(resourceIds));
        profile.getResources().addAll(resources);
        return profileMapper.toResponse(profileRepository.save(profile));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Profile", description = "Remoción de recursos del perfil")
    @Transactional
    @CacheEvict(value = "profiles", key = "#profileId")
    public ProfileResponse removeResources(UUID profileId, Set<UUID> resourceIds) {
        Profile profile = profileRepository.findByIdWithResources(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", profileId));
        profile.getResources().removeIf(r -> resourceIds.contains(r.getId()));
        return profileMapper.toResponse(profileRepository.save(profile));
    }

    @Auditable(action = AuditAction.DELETE, entityType = "Profile", description = "Desactivación de perfil de usuario")
    @Transactional
    @CacheEvict(value = "profiles", key = "#id")
    public void delete(UUID id) {
        Profile profile = profileRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", id));
        profile.setActive(false);
        profileRepository.save(profile);
    }
}
