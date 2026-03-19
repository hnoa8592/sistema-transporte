package com.transporte.usuarios.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.security.multitenancy.TenantContext;
import com.transporte.usuarios.dto.UpdateUserRequest;
import com.transporte.usuarios.dto.UserRequest;
import com.transporte.usuarios.dto.UserResponse;
import com.transporte.usuarios.entity.Profile;
import com.transporte.usuarios.entity.User;
import com.transporte.usuarios.mapper.UserMapper;
import com.transporte.usuarios.repository.ProfileRepository;
import com.transporte.usuarios.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<UserResponse> findAll(Pageable pageable) {
        return PageResponse.of(userRepository.findAllByActiveTrue(pageable).map(userMapper::toResponse));
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponse findById(UUID id) {
        return userMapper.toResponse(findUserById(id));
    }

    @Auditable(action = AuditAction.CREATE, entityType = "User", description = "Creación de nuevo usuario del sistema")
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("El nombre de usuario '" + request.username() + "' ya existe");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("El correo electrónico '" + request.email() + "' ya existe");
        }
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setTenantId(TenantContext.getCurrentTenant());
        if (request.profileId() != null) {
            Profile profile = profileRepository.findByIdAndActiveTrue(request.profileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profile", request.profileId()));
            user.setProfile(profile);
        }
        return userMapper.toResponse(userRepository.save(user));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "User", description = "Actualización de datos del usuario")
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findUserById(id);
        userMapper.updateFromRequest(request, user);
        if (request.profileId() != null) {
            Profile profile = profileRepository.findByIdAndActiveTrue(request.profileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profile", request.profileId()));
            user.setProfile(profile);
        }
        return userMapper.toResponse(userRepository.save(user));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "User", description = "Cambio de contraseña del usuario")
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void changePassword(UUID id, String currentPassword, String newPassword) {
        User user = findUserById(id);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("La contraseña actual es incorrecta");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Auditable(action = AuditAction.DELETE, entityType = "User", description = "Desactivación de usuario del sistema")
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void delete(UUID id) {
        User user = findUserById(id);
        user.setActive(false);
        userRepository.save(user);
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
