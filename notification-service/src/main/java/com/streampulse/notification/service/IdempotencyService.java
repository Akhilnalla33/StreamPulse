package com.streampulse.notification.service;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Dedupe for {@code streampulse.alerts}: {@code idempotency:alerts:{alertId}}, TTL 24h (docs/CONTRACT.md). */
@Service
public class IdempotencyService {

    private static final String KEY_PREFIX = "idempotency:alerts:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean markProcessedIfNew(String alertId) {
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + alertId, "1", TTL);
        return Boolean.TRUE.equals(wasAbsent);
    }
}
