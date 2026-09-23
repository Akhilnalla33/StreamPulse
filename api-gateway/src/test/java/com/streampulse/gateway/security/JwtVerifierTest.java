package com.streampulse.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtVerifierTest {

    private static final String SECRET = "gateway-test-secret-key-long-enough-for-hs256-alg";

    private final JwtVerifier verifier = new JwtVerifier(new JwtProperties(SECRET));

    @Test
    void verifiesValidAccessTokenAndExtractsIdentity() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String token = tokenFor(userId, tenantId, "access", List.of("ADMIN", "MEMBER"), 900);

        JwtVerifier.VerifiedIdentity identity = verifier.verify(token);

        assertThat(identity.userId()).isEqualTo(userId.toString());
        assertThat(identity.tenantId()).isEqualTo(tenantId.toString());
        assertThat(identity.roles()).containsExactlyInAnyOrder("ADMIN", "MEMBER");
    }

    @Test
    void rejectsRefreshTokenAtTheEdge() {
        String token = tokenFor(UUID.randomUUID(), UUID.randomUUID(), "refresh", List.of("MEMBER"), 604800);

        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        String token = tokenFor(UUID.randomUUID(), UUID.randomUUID(), "access", List.of("MEMBER"), -60);

        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithWrongSecret() {
        SecretKey wrongKey = Keys.hmacShaKeyFor("a-totally-different-secret-value-1234567890".getBytes());
        Instant now = Instant.now();
        String token = Jwts.builder()
                .issuer("streampulse-auth")
                .subject(UUID.randomUUID().toString())
                .claim("tenantId", UUID.randomUUID().toString())
                .claim("roles", List.of("MEMBER"))
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(900, ChronoUnit.SECONDS)))
                .signWith(wrongKey)
                .compact();

        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(JwtException.class);
    }

    private String tokenFor(UUID userId, UUID tenantId, String type, List<String> roles, long ttlSeconds) {
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        return Jwts.builder()
                .issuer("streampulse-auth")
                .subject(userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("roles", roles)
                .claim("type", type)
                .issuedAt(Date.from(now.minus(1, ChronoUnit.SECONDS)))
                .expiration(Date.from(now.plus(ttlSeconds, ChronoUnit.SECONDS)))
                .signWith(key)
                .compact();
    }
}
