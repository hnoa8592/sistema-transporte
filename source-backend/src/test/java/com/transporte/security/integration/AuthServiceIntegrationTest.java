package com.transporte.security.integration;

import com.transporte.core.exception.BusinessException;
import com.transporte.security.dto.LoginRequest;
import com.transporte.security.dto.LoginResponse;
import com.transporte.security.dto.RefreshTokenRequest;
import com.transporte.security.entity.RefreshToken;
import com.transporte.security.jwt.JwtProperties;
import com.transporte.security.jwt.JwtService;
import com.transporte.security.repository.RefreshTokenRepository;
import com.transporte.security.service.AuthService;
import com.transporte.security.service.TransporteUserDetails;
import com.transporte.tenants.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Integration-style test for the authentication flow.
 *
 * Wires the real {@link AuthService} with mocked collaborators so the
 * full business logic (tenant validation, token generation, token rotation,
 * role/permission extraction) is exercised end-to-end without requiring
 * a running database or Spring context.
 *
 * Follows the same pragmatic pattern as {@code TicketIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Integration Tests")
class AuthServiceIntegrationTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtProperties jwtProperties;
    @Mock private TenantService tenantService;

    private AuthService authService;

    private static final String TENANT_ID    = "transporte_dev";
    private static final String USERNAME     = "admin";
    private static final String PASSWORD     = "admin1234";
    private static final String ACCESS_TOKEN  = "eyJhbGciOiJIUzI1NiJ9.access.signature";
    private static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiJ9.refresh.signature";
    private static final long   EXPIRY_MS     = 900_000L;      // 15 min
    private static final long   REFRESH_MS    = 604_800_000L;  // 7 days

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager, jwtService,
                refreshTokenRepository, jwtProperties, tenantService
        );

        lenient().when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(EXPIRY_MS);
        lenient().when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(REFRESH_MS);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private TransporteUserDetails buildUserDetails(UUID userId, List<String> roles, List<String> permissions) {
        List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
        roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));

        return TransporteUserDetails.builder()
                .userId(userId)
                .username(USERNAME)
                .password("$2a$10$hashed")
                .tenantId(TENANT_ID)
                .active(true)
                .authorities(authorities)
                .build();
    }

    private Authentication buildAuthentication(TransporteUserDetails userDetails) {
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private RefreshToken buildStoredRefreshToken(UUID userId) {
        return RefreshToken.builder()
                .token(REFRESH_TOKEN)
                .userId(userId)
                .tenantId(TENANT_ID)
                .username(USERNAME)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    // -----------------------------------------------------------------------
    // Login flow
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Successful login returns access and refresh tokens with correct claims")
        void successfulLoginReturnsTokens() {
            UUID userId = UUID.randomUUID();
            TransporteUserDetails userDetails = buildUserDetails(userId, List.of("ADMIN"), List.of("PASAJES_VENTA", "FINANZAS_CAJA"));
            Authentication auth = buildAuthentication(userDetails);

            given(authenticationManager.authenticate(any())).willReturn(auth);
            given(jwtService.generateAccessToken(eq(userId), eq(USERNAME), eq(TENANT_ID), anyList(), anyList()))
                    .willReturn(ACCESS_TOKEN);
            given(jwtService.generateRefreshToken(eq(userId), eq(USERNAME), eq(TENANT_ID)))
                    .willReturn(REFRESH_TOKEN);
            given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            LoginRequest request = new LoginRequest(USERNAME, PASSWORD, TENANT_ID);
            LoginResponse response = authService.login(request);

            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.username()).isEqualTo(USERNAME);
            assertThat(response.tenantId()).isEqualTo(TENANT_ID);
            assertThat(response.roles()).containsExactly("ADMIN");
            assertThat(response.permissions()).containsExactlyInAnyOrder("PASAJES_VENTA", "FINANZAS_CAJA");
            assertThat(response.expiresIn()).isEqualTo(EXPIRY_MS / 1000);
        }

        @Test
        @DisplayName("Login validates tenant exists before authenticating")
        void loginValidatesTenantFirst() {
            UUID userId = UUID.randomUUID();
            TransporteUserDetails userDetails = buildUserDetails(userId, List.of("CAJERO"), List.of());
            Authentication auth = buildAuthentication(userDetails);

            given(authenticationManager.authenticate(any())).willReturn(auth);
            given(jwtService.generateAccessToken(any(), any(), any(), any(), any())).willReturn(ACCESS_TOKEN);
            given(jwtService.generateRefreshToken(any(), any(), any())).willReturn(REFRESH_TOKEN);
            given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            LoginRequest request = new LoginRequest(USERNAME, PASSWORD, TENANT_ID);
            authService.login(request);

            // TenantService.findBySchemaName must be called before authentication
            var inOrder = inOrder(tenantService, authenticationManager);
            inOrder.verify(tenantService).findBySchemaName(TENANT_ID);
            inOrder.verify(authenticationManager).authenticate(any());
        }

        @Test
        @DisplayName("Login sends credentials to AuthenticationManager correctly")
        void loginPassesCredentialsToAuthManager() {
            UUID userId = UUID.randomUUID();
            TransporteUserDetails userDetails = buildUserDetails(userId, List.of("ADMIN"), List.of());
            Authentication auth = buildAuthentication(userDetails);

            given(authenticationManager.authenticate(any())).willReturn(auth);
            given(jwtService.generateAccessToken(any(), any(), any(), any(), any())).willReturn(ACCESS_TOKEN);
            given(jwtService.generateRefreshToken(any(), any(), any())).willReturn(REFRESH_TOKEN);
            given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            authService.login(new LoginRequest(USERNAME, PASSWORD, TENANT_ID));

            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());
            assertThat(captor.getValue().getPrincipal()).isEqualTo(USERNAME);
            assertThat(captor.getValue().getCredentials()).isEqualTo(PASSWORD);
        }

        @Test
        @DisplayName("Login revokes old tokens before saving the new refresh token")
        void loginRevokesOldTokensBeforeSavingNew() {
            UUID userId = UUID.randomUUID();
            TransporteUserDetails userDetails = buildUserDetails(userId, List.of("ADMIN"), List.of());
            Authentication auth = buildAuthentication(userDetails);

            given(authenticationManager.authenticate(any())).willReturn(auth);
            given(jwtService.generateAccessToken(any(), any(), any(), any(), any())).willReturn(ACCESS_TOKEN);
            given(jwtService.generateRefreshToken(any(), any(), any())).willReturn(REFRESH_TOKEN);
            given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            authService.login(new LoginRequest(USERNAME, PASSWORD, TENANT_ID));

            var inOrder = inOrder(refreshTokenRepository);
            inOrder.verify(refreshTokenRepository).revokeAllByUserId(userId);
            inOrder.verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Login saves refresh token with correct metadata")
        void loginSavesRefreshTokenWithMetadata() {
            UUID userId = UUID.randomUUID();
            TransporteUserDetails userDetails = buildUserDetails(userId, List.of("ADMIN"), List.of());
            Authentication auth = buildAuthentication(userDetails);

            given(authenticationManager.authenticate(any())).willReturn(auth);
            given(jwtService.generateAccessToken(any(), any(), any(), any(), any())).willReturn(ACCESS_TOKEN);
            given(jwtService.generateRefreshToken(any(), any(), any())).willReturn(REFRESH_TOKEN);

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            given(refreshTokenRepository.save(tokenCaptor.capture())).willAnswer(inv -> inv.getArgument(0));

            authService.login(new LoginRequest(USERNAME, PASSWORD, TENANT_ID));

            RefreshToken saved = tokenCaptor.getValue();
            assertThat(saved.getToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(saved.getUsername()).isEqualTo(USERNAME);
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("Login throws when tenant does not exist")
        void loginThrowsWhenTenantNotFound() {
            given(tenantService.findBySchemaName(TENANT_ID))
                    .willThrow(new BusinessException("Tenant not found", org.springframework.http.HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> authService.login(new LoginRequest(USERNAME, PASSWORD, TENANT_ID)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Tenant not found");

            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("Login throws when credentials are invalid")
        void loginThrowsOnBadCredentials() {
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(new LoginRequest(USERNAME, "wrong_pass", TENANT_ID)))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Login throws FORBIDDEN when user does not belong to the requested tenant")
        void loginThrowsWhenUserBelongsToOtherTenant() {
            UUID userId = UUID.randomUUID();
            // User was authenticated but belongs to a different tenant
            TransporteUserDetails userDetails = TransporteUserDetails.builder()
                    .userId(userId).username(USERNAME).password("$2a$10$hashed")
                    .tenantId("other_tenant").active(true)
                    .authorities(List.of((org.springframework.security.core.GrantedAuthority) new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            given(authenticationManager.authenticate(any()))
                    .willReturn(buildAuthentication(userDetails));

            assertThatThrownBy(() -> authService.login(new LoginRequest(USERNAME, PASSWORD, TENANT_ID)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no pertenece a esta empresa");
        }
    }

    // -----------------------------------------------------------------------
    // Token refresh flow
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Token Refresh")
    class TokenRefreshTests {

        @Test
        @DisplayName("Refresh with valid token returns new access and refresh tokens")
        void refreshWithValidTokenReturnsNewTokens() {
            UUID userId = UUID.randomUUID();
            RefreshToken storedToken = buildStoredRefreshToken(userId);

            given(refreshTokenRepository.findByToken(REFRESH_TOKEN)).willReturn(Optional.of(storedToken));
            given(jwtService.isTokenValid(REFRESH_TOKEN)).willReturn(true);
            given(jwtService.extractTokenType(REFRESH_TOKEN)).willReturn("REFRESH");
            given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(jwtService.generateAccessToken(any(), any(), any(), any(), any())).willReturn("new-access-token");
            given(jwtService.generateRefreshToken(any(), any(), any())).willReturn("new-refresh-token");

            LoginResponse response = authService.refreshToken(new RefreshTokenRequest(REFRESH_TOKEN));

            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
            assertThat(response.username()).isEqualTo(USERNAME);
            assertThat(response.tenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Refresh marks old token as revoked (token rotation)")
        void refreshRevokesOldToken() {
            UUID userId = UUID.randomUUID();
            RefreshToken storedToken = buildStoredRefreshToken(userId);

            given(refreshTokenRepository.findByToken(REFRESH_TOKEN)).willReturn(Optional.of(storedToken));
            given(jwtService.isTokenValid(REFRESH_TOKEN)).willReturn(true);
            given(jwtService.extractTokenType(REFRESH_TOKEN)).willReturn("REFRESH");
            given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(jwtService.generateAccessToken(any(), any(), any(), any(), any())).willReturn("new-access-token");
            given(jwtService.generateRefreshToken(any(), any(), any())).willReturn("new-refresh-token");

            authService.refreshToken(new RefreshTokenRequest(REFRESH_TOKEN));

            // The old token must be saved as revoked
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository, atLeastOnce()).save(captor.capture());
            boolean foundRevoked = captor.getAllValues().stream()
                    .anyMatch(RefreshToken::isRevoked);
            assertThat(foundRevoked).isTrue();
        }

        @Test
        @DisplayName("Refresh throws UNAUTHORIZED when token is not found")
        void refreshThrowsWhenTokenNotFound() {
            given(refreshTokenRepository.findByToken(anyString())).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("unknown-token")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Token de actualización inválido");
        }

        @Test
        @DisplayName("Refresh throws UNAUTHORIZED when stored token is expired or revoked")
        void refreshThrowsWhenTokenExpiredOrRevoked() {
            UUID userId = UUID.randomUUID();
            RefreshToken expiredToken = RefreshToken.builder()
                    .token(REFRESH_TOKEN)
                    .userId(userId)
                    .tenantId(TENANT_ID)
                    .username(USERNAME)
                    .expiresAt(LocalDateTime.now().minusDays(1)) // expired
                    .build();

            given(refreshTokenRepository.findByToken(REFRESH_TOKEN)).willReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(REFRESH_TOKEN)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("expirado o fue revocado");
        }

        @Test
        @DisplayName("Refresh throws UNAUTHORIZED when JWT signature is invalid")
        void refreshThrowsWhenJwtInvalid() {
            UUID userId = UUID.randomUUID();
            RefreshToken storedToken = buildStoredRefreshToken(userId);

            given(refreshTokenRepository.findByToken(REFRESH_TOKEN)).willReturn(Optional.of(storedToken));
            given(jwtService.isTokenValid(REFRESH_TOKEN)).willReturn(false);

            assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(REFRESH_TOKEN)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Token de actualización inválido");
        }

        @Test
        @DisplayName("Refresh throws UNAUTHORIZED when token type is not REFRESH")
        void refreshThrowsWhenTokenTypeIsNotRefresh() {
            UUID userId = UUID.randomUUID();
            RefreshToken storedToken = buildStoredRefreshToken(userId);

            given(refreshTokenRepository.findByToken(REFRESH_TOKEN)).willReturn(Optional.of(storedToken));
            given(jwtService.isTokenValid(REFRESH_TOKEN)).willReturn(true);
            given(jwtService.extractTokenType(REFRESH_TOKEN)).willReturn("ACCESS"); // wrong type

            assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(REFRESH_TOKEN)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Token de actualización inválido");
        }
    }

    // -----------------------------------------------------------------------
    // Logout flow
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("Logout marks the stored refresh token as revoked")
        void logoutRevokesRefreshToken() {
            UUID userId = UUID.randomUUID();
            RefreshToken storedToken = buildStoredRefreshToken(userId);

            given(refreshTokenRepository.findByToken(REFRESH_TOKEN)).willReturn(Optional.of(storedToken));
            given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            authService.logout(REFRESH_TOKEN);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            assertThat(captor.getValue().isRevoked()).isTrue();
        }

        @Test
        @DisplayName("Logout is a no-op when token does not exist in the store")
        void logoutIsNoOpWhenTokenNotFound() {
            given(refreshTokenRepository.findByToken(anyString())).willReturn(Optional.empty());

            assertThatCode(() -> authService.logout("unknown-token"))
                    .doesNotThrowAnyException();

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // Role and permission extraction
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Role and permission extraction from JWT authorities")
    class AuthoritiesExtractionTests {

        private LoginResponse doLogin(List<String> roles, List<String> permissions) {
            UUID userId = UUID.randomUUID();
            TransporteUserDetails userDetails = buildUserDetails(userId, roles, permissions);
            given(authenticationManager.authenticate(any())).willReturn(buildAuthentication(userDetails));
            given(jwtService.generateAccessToken(any(), any(), any(), any(), any())).willReturn(ACCESS_TOKEN);
            given(jwtService.generateRefreshToken(any(), any(), any())).willReturn(REFRESH_TOKEN);
            given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            return authService.login(new LoginRequest(USERNAME, PASSWORD, TENANT_ID));
        }

        @Test
        @DisplayName("ROLE_ prefix is stripped when building roles list")
        void rolePrefixIsStripped() {
            LoginResponse r = doLogin(List.of("ADMIN", "CAJERO"), List.of());
            assertThat(r.roles()).containsExactlyInAnyOrder("ADMIN", "CAJERO");
            // Should NOT contain "ROLE_" prefix
            assertThat(r.roles()).noneMatch(role -> role.startsWith("ROLE_"));
        }

        @Test
        @DisplayName("Permissions do not include ROLE_ prefixed authorities")
        void permissionsExcludeRoles() {
            LoginResponse r = doLogin(List.of("ADMIN"), List.of("PASAJES_VENTA", "ENCOMIENDAS_GESTION"));
            assertThat(r.permissions()).containsExactlyInAnyOrder("PASAJES_VENTA", "ENCOMIENDAS_GESTION");
            assertThat(r.permissions()).noneMatch(p -> p.startsWith("ROLE_"));
        }

        @Test
        @DisplayName("User with no permissions returns empty permissions list")
        void userWithNoPermissionsHasEmptyList() {
            LoginResponse r = doLogin(List.of("VIEWER"), List.of());
            assertThat(r.permissions()).isEmpty();
            assertThat(r.roles()).containsExactly("VIEWER");
        }
    }
}
