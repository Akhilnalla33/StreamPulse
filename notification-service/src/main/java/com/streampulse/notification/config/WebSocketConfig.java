package com.streampulse.notification.config;

import com.streampulse.notification.websocket.NotificationWebSocketHandler;
import com.streampulse.notification.websocket.TenantHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler handler;
    private final TenantHandshakeInterceptor interceptor;

    public WebSocketConfig(NotificationWebSocketHandler handler, TenantHandshakeInterceptor interceptor) {
        this.handler = handler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/notifications")
                .addInterceptors(interceptor)
                .setAllowedOrigins("*");
    }
}
