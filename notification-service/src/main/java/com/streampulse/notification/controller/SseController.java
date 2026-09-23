package com.streampulse.notification.controller;

import com.streampulse.notification.exception.MissingTenantException;
import com.streampulse.notification.service.SseEmitterRegistry;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/sse")
public class SseController {

    private final SseEmitterRegistry registry;

    public SseController(SseEmitterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER', 'VIEWER')")
    public SseEmitter subscribe(@RequestHeader("X-Tenant-Id") String tenantIdHeader) {
        if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
            throw new MissingTenantException();
        }
        return registry.register(tenantIdHeader);
    }
}
