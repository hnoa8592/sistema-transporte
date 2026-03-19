package com.transporte.security.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAuthPort {

    record UserAuthData(
            UUID userId,
            String username,
            String password,
            String tenantId,
            boolean active,
            List<String> roles,
            List<String> permissions
    ) {}

    Optional<UserAuthData> findByUsername(String username);
}
