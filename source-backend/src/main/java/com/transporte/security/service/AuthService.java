package com.transporte.security.service;

import com.transporte.core.exception.BusinessException;
import com.transporte.security.dto.LoginRequest;
import com.transporte.security.dto.LoginResponse;
import com.transporte.security.dto.RefreshTokenRequest;
import com.transporte.security.entity.RefreshToken;
import com.transporte.security.jwt.JwtProperties;
import com.transporte.security.jwt.JwtService;
import com.transporte.security.multitenancy.TenantContext;
import com.transporte.security.repository.RefreshTokenRepository;
import com.transporte.tenants.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final TenantService tenantService;

    // NOTE: NOT @Transactional — the JpaTransactionManager opens the Hibernate Session
    // (and resolves the tenant identifier) BEFORE the method body runs. If this method
    // were transactional, TenantContext would still be empty at Session creation time
    // and all queries would target the 'public' schema instead of the tenant schema.
    // Each inner call manages its own transaction with the correct schema already set.
    public LoginResponse login(LoginRequest request) {

        TenantContext.setCurrentTenant(request.tenantId());

        // Validate tenant exists (TenantService is @Transactional(readOnly=true), uses public schema)
        tenantService.findBySchemaName(request.tenantId());

        // Authenticate: UserAuthPortImpl.findByUsername is @Transactional(readOnly=true)
        // and opens its Session AFTER TenantContext is set → correct tenant schema
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        TransporteUserDetails userDetails = (TransporteUserDetails) authentication.getPrincipal();

        if (!request.tenantId().equals(userDetails.getTenantId())) {
            throw new BusinessException("El usuario no pertenece a esta empresa", HttpStatus.FORBIDDEN);
        }

        List<String> roles = userDetails.getAuthorities().stream()
                .filter(a -> a.getAuthority().startsWith("ROLE_"))
                .map(a -> a.getAuthority().substring(5))
                .toList();

        List<String> permissions = userDetails.getAuthorities().stream()
                .filter(a -> !a.getAuthority().startsWith("ROLE_"))
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = jwtService.generateAccessToken(
                userDetails.getUserId(), userDetails.getUsername(),
                userDetails.getTenantId(), roles, permissions
        );
        String refreshTokenStr = jwtService.generateRefreshToken(
                userDetails.getUserId(), userDetails.getUsername(), userDetails.getTenantId()
        );

        // Revoke old tokens and save new one (each is its own @Transactional call on the repo)
        refreshTokenRepository.revokeAllByUserId(userDetails.getUserId());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .userId(userDetails.getUserId())
                .tenantId(userDetails.getTenantId())
                .username(userDetails.getUsername())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirationMs() / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);

        log.info("Usuario {} autenticado en la empresa {}", userDetails.getUsername(), userDetails.getTenantId());

        return LoginResponse.of(
                accessToken, refreshTokenStr,
                jwtProperties.getAccessTokenExpirationMs() / 1000,
                userDetails.getUserId().toString(), userDetails.getUsername(),
                userDetails.getTenantId(), roles, permissions
        );
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException("Token de actualización inválido", HttpStatus.UNAUTHORIZED));

        if (!storedToken.isValid()) {
            throw new BusinessException("El token de actualización ha expirado o fue revocado", HttpStatus.UNAUTHORIZED);
        }

        // Verify JWT refresh token
        if (!jwtService.isTokenValid(storedToken.getToken()) ||
                !"REFRESH".equals(jwtService.extractTokenType(storedToken.getToken()))) {
            throw new BusinessException("Token de actualización inválido", HttpStatus.UNAUTHORIZED);
        }

        // Revoke old refresh token (rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(
                storedToken.getUserId(), storedToken.getUsername(),
                storedToken.getTenantId(), List.of(), List.of()
        );
        String newRefreshTokenStr = jwtService.generateRefreshToken(
                storedToken.getUserId(), storedToken.getUsername(), storedToken.getTenantId()
        );

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(newRefreshTokenStr)
                .userId(storedToken.getUserId())
                .tenantId(storedToken.getTenantId())
                .username(storedToken.getUsername())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirationMs() / 1000))
                .build();
        refreshTokenRepository.save(newRefreshToken);

        return LoginResponse.of(
                newAccessToken, newRefreshTokenStr,
                jwtProperties.getAccessTokenExpirationMs() / 1000,
                storedToken.getUserId().toString(), storedToken.getUsername(),
                storedToken.getTenantId(), List.of(), List.of()
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

}
