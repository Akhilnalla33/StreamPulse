package com.streampulse.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "streampulse.jwt")
public record JwtProperties(String secret, long accessTokenTtlSeconds, long refreshTokenTtlSeconds) {
}
