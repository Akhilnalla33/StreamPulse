package com.streampulse.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.gateway.security.JwtProperties;
import com.streampulse.gateway.security.JwtVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class JwtAuthenticationGlobalFilterTest {

    private static final String SECRET = "gateway-filter-test-secret-key-long-enough-hs256";

    private final JwtVerifier verifier = new JwtVerifier(new JwtProperties(SECRET));
    private final JwtAuthenticationGlobalFilter filter = new JwtAuthenticationGlobalFilter(verifier, new ObjectMapper());

    @Test
    void allowsPublicAuthRouteWithoutToken() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login").build());

        boolean[] chainCalled = {false};
        filter.filter(exchange, (ex) -> {
            chainCalled[0] = true;
            return Mono.empty();
        }).block();

        assertThat(chainCalled[0]).isTrue();
    }

    @Test
    void rejectsProtectedRouteWithoutToken() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/events").build());

        filter.filter(exchange, (ex) -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void forwardsIdentityHeadersForValidToken() {
        UUID tenantId = UUID.randomUUID();
        String token = validAccessToken(tenantId);
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/events").header("Authorization", "Bearer " + token).build());

        String[] forwardedTenant = {null};
        filter.filter(exchange, (ex) -> {
            forwardedTenant[0] = ex.getRequest().getHeaders().getFirst("X-Tenant-Id");
            return Mono.empty();
        }).block();

        assertThat(forwardedTenant[0]).isEqualTo(tenantId.toString());
    }

    private String validAccessToken(UUID tenantId) {
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        return Jwts.builder()
                .issuer("streampulse-auth")
                .subject(UUID.randomUUID().toString())
                .claim("tenantId", tenantId.toString())
                .claim("roles", List.of("MEMBER"))
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(900, ChronoUnit.SECONDS)))
                .signWith(key)
                .compact();
    }
}
