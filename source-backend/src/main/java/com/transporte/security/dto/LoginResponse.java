package com.transporte.security.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String userId,
        String username,
        String tenantId,
        List<String> roles,
        List<String> permissions
) {
    public static LoginResponse of(String accessToken, String refreshToken, long expiresIn,
                                    String userId, String username, String tenantId,
                                    List<String> roles, List<String> permissions) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn,
                userId, username, tenantId, roles, permissions);
    }
}
