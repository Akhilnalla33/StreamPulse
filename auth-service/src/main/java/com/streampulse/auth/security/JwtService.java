package com.streampulse.auth.security;

import com.streampulse.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Issues and verifies HS256 JWTs. The signing key and claim layout here are the single source
 * of truth: api-gateway performs the same local verification (no network call back to
 * auth-service) using the identical {@code JWT_SECRET}, so any change here must be mirrored
 * there (see {@code docs/CONTRACT.md}).
 */
@Component
public final class JwtService {

    static final String ISSUER = "streampulse-auth";
    static final String CLAIM_TENANT_ID = "tenantId";
    static final String CLAIM_ROLES = "roles";
    static final String CLAIM_TYPE = "type";

    private final Key signingKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, UUID tenantId, Set<Role> roles) {
        return generateToken(userId, tenantId, roles, TokenType.ACCESS, properties.accessTokenTtlSeconds());
    }

    public String generateRefreshToken(UUID userId, UUID tenantId, Set<Role> roles) {
        return generateToken(userId, tenantId, roles, TokenType.REFRESH, properties.refreshTokenTtlSeconds());
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtlSeconds();
    }

    public long refreshTokenTtlSeconds() {
        return properties.refreshTokenTtlSeconds();
    }

    private String generateToken(UUID userId, UUID tenantId, Set<Role> roles, TokenType type, long ttlSeconds) {
        Instant now = Instant.now();
        List<String> roleNames = roles.stream().map(Enum::name).collect(Collectors.toList());
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .claim(CLAIM_TENANT_ID, tenantId.toString())
                .claim(CLAIM_ROLES, roleNames)
                .claim(CLAIM_TYPE, type.name().toLowerCase())
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(ttlSeconds, ChronoUnit.SECONDS)))
                .signWith(signingKey)
                .compact();
    }

    /** Parses and verifies a token's signature and expiry, throwing {@link JwtException} if invalid. */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        return TokenType.REFRESH.name().equalsIgnoreCase(claims.get(CLAIM_TYPE, String.class));
    }
}
