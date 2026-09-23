package com.streampulse.notification.websocket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Tracks connected WebSocket sessions per tenant so a broadcast can be scoped to exactly one
 * tenant's clients. This is the mechanism the multi-tenant isolation test in
 * {@code NotificationBroadcastServiceTest} exercises.
 */
@Component
public class TenantSessionRegistry {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByTenant = new ConcurrentHashMap<>();

    public void register(String tenantId, WebSocketSession session) {
        sessionsByTenant.computeIfAbsent(tenantId, t -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unregister(String tenantId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByTenant.get(tenantId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    public Set<WebSocketSession> sessionsFor(String tenantId) {
        return sessionsByTenant.getOrDefault(tenantId, Set.of());
    }
}
