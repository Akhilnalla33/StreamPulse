package com.streampulse.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.contracts.error.ErrorResponse;
import com.streampulse.gateway.security.JwtVerifier;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Validates the {@code Authorization: Bearer} token locally for every route except the public
 * ones, then forwards identity as {@code X-Tenant-Id}/{@code X-User-Id}/{@code X-User-Roles}
 * headers so downstream services don't need to re-parse the JWT to know who's calling (they
 * still independently verify the JWT themselves for defense in depth, see CONTRACT.md).
 */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String ROLES_HEADER = "X-User-Roles";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/v1/auth/", "/actuator/", "/v3/api-docs", "/swagger-ui");

    private final JwtVerifier jwtVerifier;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationGlobalFilter(JwtVerifier jwtVerifier, ObjectMapper objectMapper) {
        this.jwtVerifier = jwtVerifier;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        if (isPublic(path)) {
            ServerHttpRequest mutated = request.mutate().header(CORRELATION_ID_HEADER, correlationId).build();
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing bearer token", correlationId);
        }

        try {
            JwtVerifier.VerifiedIdentity identity = jwtVerifier.verify(authHeader.substring("Bearer ".length()));
            ServerHttpRequest mutated = request.mutate()
                    .header(TENANT_ID_HEADER, identity.tenantId())
                    .header(USER_ID_HEADER, identity.userId())
                    .header(ROLES_HEADER, String.join(",", identity.roles()))
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .build();
            exchange.getAttributes().put("tenantId", identity.tenantId());
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException e) {
            return unauthorized(exchange, "Invalid or expired token", correlationId);
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message, String correlationId) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse body = new ErrorResponse(
                Instant.now(), 401, "Unauthorized", message,
                exchange.getRequest().getURI().getPath(), correlationId, List.of());
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
