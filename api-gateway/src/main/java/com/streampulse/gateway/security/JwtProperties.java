package com.streampulse.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "streampulse.jwt")
public record JwtProperties(String secret) {
}
