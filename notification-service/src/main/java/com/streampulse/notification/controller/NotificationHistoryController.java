package com.streampulse.notification.controller;

import com.streampulse.notification.dto.NotificationResponse;
import com.streampulse.notification.exception.MissingTenantException;
import com.streampulse.notification.repository.NotificationRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationHistoryController {

    private final NotificationRepository repository;

    public NotificationHistoryController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER', 'VIEWER')")
    public Page<NotificationResponse> history(@RequestHeader("X-Tenant-Id") String tenantIdHeader, Pageable pageable) {
        if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
            throw new MissingTenantException();
        }
        return repository.findByTenantIdOrderByFiredAtDesc(UUID.fromString(tenantIdHeader), pageable)
                .map(NotificationResponse::from);
    }
}
