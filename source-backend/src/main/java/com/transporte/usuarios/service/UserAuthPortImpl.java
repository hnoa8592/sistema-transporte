package com.transporte.usuarios.service;

import com.transporte.security.port.UserAuthPort;
import com.transporte.tenants.repository.TenantRepository;
import com.transporte.tenants.service.TenantService;
import com.transporte.usuarios.entity.Resource;
import com.transporte.usuarios.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAuthPortImpl implements UserAuthPort {

    private final UserRepository userRepository;
    private final TenantService tenantService;

    @Override
    public Optional<UserAuthData> findByUsername(String username) {
        return userRepository.findByUsernameWithProfileAndResources(username)
                .map(user -> new UserAuthData(
                        user.getId(),
                        user.getUsername(),
                        user.getPassword(),
                        tenantService.findBySchemaName(user.getTenantId()).schemaName(),
                        user.isActive(),
                        List.of("USER"),
                        user.getProfile() != null ?
                                user.getProfile().getResources().stream()
                                        .filter(Resource::isActive)
                                        .map(r -> r.getHttpMethod() + ":" + r.getEndpoint())
                                        .toList() :
                                List.of()
                ));
    }
}
