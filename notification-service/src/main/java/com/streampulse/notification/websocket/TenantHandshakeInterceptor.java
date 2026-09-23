package com.streampulse.notification.websocket;

import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Reads the tenant id from the {@code X-Tenant-Id} header set by api-gateway (see
 * docs/CONTRACT.md) and stashes it on the session so {@link NotificationWebSocketHandler} can
 * register the connection under the right tenant. Rejects the handshake if the header is
 * absent, i.e. the request didn't come through the gateway's JWT check.
 */
@Component
public class TenantHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String tenantId = request.getHeaders().getFirst("X-Tenant-Id");
        if (tenantId == null || tenantId.isBlank()) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(NotificationWebSocketHandler.TENANT_ATTR, tenantId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
