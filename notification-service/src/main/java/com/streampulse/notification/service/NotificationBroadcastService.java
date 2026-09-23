package com.streampulse.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.contracts.event.AlertEvent;
import com.streampulse.notification.dto.NotificationResponse;
import com.streampulse.notification.entity.Notification;
import com.streampulse.notification.repository.NotificationRepository;
import com.streampulse.notification.websocket.TenantSessionRegistry;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Consumes {@link AlertEvent}s from {@code streampulse.alerts}, persists them as
 * {@link Notification} history, and pushes them to every connected client of the owning tenant
 * over WebSocket and SSE. Delivery is strictly scoped to {@code event.tenantId()} — never
 * broadcast to all tenants — which is what the multi-tenant isolation test verifies.
 */
@Service
public class NotificationBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(NotificationBroadcastService.class);
    private static final String ALERTS_TOPIC = "streampulse.alerts";

    private final NotificationRepository repository;
    private final IdempotencyService idempotencyService;
    private final TenantSessionRegistry sessionRegistry;
    private final SseEmitterRegistry sseRegistry;
    private final ObjectMapper objectMapper;

    public NotificationBroadcastService(NotificationRepository repository, IdempotencyService idempotencyService,
                                         TenantSessionRegistry sessionRegistry, SseEmitterRegistry sseRegistry,
                                         ObjectMapper objectMapper) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.sessionRegistry = sessionRegistry;
        this.sseRegistry = sseRegistry;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = ALERTS_TOPIC, groupId = "notification-service")
    public void onAlertEvent(AlertEvent event) {
        if (!idempotencyService.markProcessedIfNew(event.alertId())) {
            log.debug("Skipping already-processed alert {}", event.alertId());
            return;
        }
        handle(event);
    }

    @Transactional
    void handle(AlertEvent event) {
        repository.save(Notification.fromAlertEvent(event));
        broadcast(event);
    }

    private void broadcast(AlertEvent event) {
        NotificationResponse payload = NotificationResponse.from(event);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize alert {} for broadcast", event.alertId(), e);
            return;
        }

        for (WebSocketSession session : sessionRegistry.sessionsFor(event.tenantId())) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                log.warn("Failed to deliver alert {} to a websocket session for tenant {}", event.alertId(), event.tenantId());
            }
        }

        List<SseEmitter> emitters = sseRegistry.emittersFor(event.tenantId());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("alert").data(json));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }
}
