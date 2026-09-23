package com.streampulse.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.streampulse.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties("test-secret-key-that-is-long-enough-for-hs256-please", 900, 604800);
        jwtService = new JwtService(properties);
    }

    @Test
    void generatesAccessTokenWithExpectedClaims() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, tenantId, Set.of(Role.ADMIN, Role.MEMBER));
        Claims claims = jwtService.parseAndValidate(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("tenantId", String.class)).isEqualTo(tenantId.toString());
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(jwtService.isRefreshToken(claims)).isFalse();
    }

    @Test
    void generatesRefreshTokenMarkedAsRefreshType() {
        String token = jwtService.generateRefreshToken(UUID.randomUUID(), UUID.randomUUID(), Set.of(Role.VIEWER));
        Claims claims = jwtService.parseAndValidate(token);

        assertThat(jwtService.isRefreshToken(claims)).isTrue();
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtService other = new JwtService(new JwtProperties("a-completely-different-secret-key-value-1234", 900, 604800));
        String token = other.generateAccessToken(UUID.randomUUID(), UUID.randomUUID(), Set.of(Role.MEMBER));

        assertThatThrownBy(() -> jwtService.parseAndValidate(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> jwtService.parseAndValidate("not-a-jwt")).isInstanceOf(JwtException.class);
    }
}
