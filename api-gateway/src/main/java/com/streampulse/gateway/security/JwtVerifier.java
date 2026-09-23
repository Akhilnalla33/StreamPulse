package com.streampulse.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Verifies access tokens locally using the shared {@code JWT_SECRET} — no network round trip
 * to auth-service. Claim layout must stay identical to {@code auth-service}'s {@code JwtService}
 * (see {@code docs/CONTRACT.md}): issuer {@code streampulse-auth}, claims {@code tenantId},
 * {@code roles}, {@code type}.
 */
@Component
public class JwtVerifier {

    private static final String ISSUER = "streampulse-auth";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";

    private final SecretKey signingKey;

    public JwtVerifier(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /** Parses and verifies signature/expiry/issuer, and rejects refresh tokens (access-only at the edge). */
    public VerifiedIdentity verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String type = claims.get(CLAIM_TYPE, String.class);
        if (!"access".equals(type)) {
            throw new io.jsonwebtoken.JwtException("Refresh tokens cannot be used for API access");
        }

        String userId = claims.getSubject();
        String tenantId = claims.get(CLAIM_TENANT_ID, String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get(CLAIM_ROLES, List.class);
        return new VerifiedIdentity(userId, tenantId, roles);
    }

    public record VerifiedIdentity(String userId, String tenantId, List<String> roles) {
    }
}
