package com.streampulse.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Resolves the Redis rate-limit bucket key to the tenant id set by
 * {@link com.streampulse.gateway.filter.JwtAuthenticationGlobalFilter}, so limits are enforced
 * per tenant rather than globally. Unauthenticated (public) requests share a single "anonymous"
 * bucket.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver tenantKeyResolver() {
        return exchange -> {
            Object tenantId = exchange.getAttribute("tenantId");
            return Mono.just(tenantId != null ? tenantId.toString() : "anonymous");
        };
    }
}
