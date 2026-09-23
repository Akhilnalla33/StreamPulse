package com.streampulse.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.streampulse.auth.dto.LoginRequest;
import com.streampulse.auth.dto.RegisterTenantRequest;
import com.streampulse.auth.entity.Role;
import com.streampulse.auth.entity.Tenant;
import com.streampulse.auth.entity.User;
import com.streampulse.auth.exception.AuthException;
import com.streampulse.auth.repository.RefreshTokenRepository;
import com.streampulse.auth.repository.TenantRepository;
import com.streampulse.auth.repository.UserRepository;
import com.streampulse.auth.security.JwtProperties;
import com.streampulse.auth.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(new JwtProperties("test-secret-key-long-enough-for-hs256-alg", 900, 604800));

    private AuthService authService;

    @BeforeEach
    void wireRealCollaborators() {
        authService = new AuthService(tenantRepository, userRepository, refreshTokenRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerTenantRejectsDuplicateSlug() {
        when(tenantRepository.existsBySlug("acme")).thenReturn(true);
        RegisterTenantRequest request = new RegisterTenantRequest("Acme", "acme", "admin@acme.io", "password123");

        assertThatThrownBy(() -> authService.registerTenant(request)).isInstanceOf(AuthException.class);
    }

    @Test
    void registerTenantIssuesTokenPairForNewAdmin() {
        when(tenantRepository.existsBySlug("acme")).thenReturn(false);
        RegisterTenantRequest request = new RegisterTenantRequest("Acme", "acme", "admin@acme.io", "password123");

        var response = authService.registerTenant(request);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void loginRejectsUnknownTenant() {
        when(tenantRepository.findBySlug("ghost")).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest("a@b.com", "password123", "ghost");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(AuthException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant(tenantId, "Acme", "acme", Instant.now());
        User user = new User(UUID.randomUUID(), tenantId, "a@b.com", passwordEncoder.encode("correct-password"), Set.of(Role.MEMBER), Instant.now());

        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantIdAndEmail(tenantId, "a@b.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("a@b.com", "wrong-password", "acme");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(AuthException.class);
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant(tenantId, "Acme", "acme", Instant.now());
        User user = new User(UUID.randomUUID(), tenantId, "a@b.com", passwordEncoder.encode("correct-password"), Set.of(Role.MEMBER), Instant.now());

        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantIdAndEmail(tenantId, "a@b.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("a@b.com", "correct-password", "acme");
        var response = authService.login(request);

        assertThat(response.accessToken()).isNotBlank();
    }
}
