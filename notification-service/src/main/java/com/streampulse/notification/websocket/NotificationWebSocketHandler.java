package com.streampulse.notification.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    public static final String TENANT_ATTR = "tenantId";

    private final TenantSessionRegistry registry;

    public NotificationWebSocketHandler(TenantSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String tenantId = (String) session.getAttributes().get(TENANT_ATTR);
        if (tenantId != null) {
            registry.register(tenantId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String tenantId = (String) session.getAttributes().get(TENANT_ATTR);
        if (tenantId != null) {
            registry.unregister(tenantId, session);
        }
    }
}
