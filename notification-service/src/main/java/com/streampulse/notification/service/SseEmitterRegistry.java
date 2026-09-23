package com.streampulse.notification.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** SSE fallback for clients that can't use WebSocket, scoped per tenant like {@code TenantSessionRegistry}. */
@Component
public class SseEmitterRegistry {

    private final ConcurrentHashMap<String, List<SseEmitter>> emittersByTenant = new ConcurrentHashMap<>();

    public SseEmitter register(String tenantId) {
        SseEmitter emitter = new SseEmitter(0L);
        List<SseEmitter> emitters = emittersByTenant.computeIfAbsent(tenantId, t -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public List<SseEmitter> emittersFor(String tenantId) {
        return emittersByTenant.getOrDefault(tenantId, List.of());
    }
}
