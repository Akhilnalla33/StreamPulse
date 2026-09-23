package com.streampulse.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.contracts.event.AlertEvent;
import com.streampulse.contracts.event.AlertSeverity;
import com.streampulse.notification.entity.Notification;
import com.streampulse.notification.repository.NotificationRepository;
import com.streampulse.notification.websocket.TenantSessionRegistry;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Proves multi-tenant WebSocket delivery isolation: a session registered under tenant A must
 * never receive a message for an alert belonging to tenant B.
 */
class NotificationBroadcastServiceTest {

    private NotificationRepository repository;
    private IdempotencyService idempotencyService;
    private TenantSessionRegistry sessionRegistry;
    private SseEmitterRegistry sseRegistry;
    private NotificationBroadcastService broadcastService;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        idempotencyService = mock(IdempotencyService.class);
        sessionRegistry = new TenantSessionRegistry();
        sseRegistry = mock(SseEmitterRegistry.class);
        when(sseRegistry.emittersFor(anyString())).thenReturn(java.util.List.of());
        when(idempotencyService.markProcessedIfNew(anyString())).thenReturn(true);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        broadcastService = new NotificationBroadcastService(repository, idempotencyService, sessionRegistry, sseRegistry, objectMapper);
    }

    @Test
    void deliversOnlyToSessionsOfOwningTenant() throws IOException {
        String tenantA = UUID.randomUUID().toString();
        String tenantB = UUID.randomUUID().toString();

        WebSocketSession sessionA = openSession();
        WebSocketSession sessionB = openSession();
        sessionRegistry.register(tenantA, sessionA);
        sessionRegistry.register(tenantB, sessionB);

        AlertEvent alertForTenantA = new AlertEvent(
                "alert-1", tenantA, "rule-1", "High CPU", AlertSeverity.CRITICAL,
                "cpu.usage.percent", 90.0, 97.0, "evt-1", "CPU too high", Instant.now());

        broadcastService.onAlertEvent(alertForTenantA);

        verify(sessionA).sendMessage(any(TextMessage.class));
        verify(sessionB, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void persistsNotificationHistoryOnDelivery() {
        String tenantId = UUID.randomUUID().toString();
        AlertEvent alertEvent = new AlertEvent(
                "alert-2", tenantId, "rule-2", "Low disk", AlertSeverity.WARNING,
                "disk.free.percent", 10.0, 5.0, "evt-2", "Disk low", Instant.now());

        broadcastService.onAlertEvent(alertEvent);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAlertId()).isEqualTo("alert-2");
        assertThat(captor.getValue().getTenantId()).isEqualTo(UUID.fromString(tenantId));
    }

    @Test
    void skipsAlreadyProcessedAlert() {
        when(idempotencyService.markProcessedIfNew("dup-alert")).thenReturn(false);
        AlertEvent alertEvent = new AlertEvent(
                "dup-alert", UUID.randomUUID().toString(), "rule-3", "Dup", AlertSeverity.INFO,
                "metric", 1.0, 2.0, "evt-3", "msg", Instant.now());

        broadcastService.onAlertEvent(alertEvent);

        verify(repository, never()).save(any());
    }

    private WebSocketSession openSession() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
