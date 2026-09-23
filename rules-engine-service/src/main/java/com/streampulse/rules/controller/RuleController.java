package com.streampulse.rules.controller;

import com.streampulse.rules.dto.RuleRequest;
import com.streampulse.rules.dto.RuleResponse;
import com.streampulse.rules.entity.AlertRule;
import com.streampulse.rules.exception.MissingTenantException;
import com.streampulse.rules.service.RuleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER', 'VIEWER')")
    public List<RuleResponse> list(@RequestHeader("X-Tenant-Id") String tenantIdHeader) {
        return ruleService.listAll(tenantId(tenantIdHeader)).stream().map(RuleResponse::from).toList();
    }

    @GetMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER', 'VIEWER')")
    public RuleResponse get(@RequestHeader("X-Tenant-Id") String tenantIdHeader, @PathVariable UUID ruleId) {
        return RuleResponse.from(ruleService.get(tenantId(tenantIdHeader), ruleId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RuleResponse> create(@RequestHeader("X-Tenant-Id") String tenantIdHeader,
                                                @Valid @RequestBody RuleRequest request) {
        AlertRule created = ruleService.create(tenantId(tenantIdHeader), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RuleResponse.from(created));
    }

    @PutMapping("/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public RuleResponse update(@RequestHeader("X-Tenant-Id") String tenantIdHeader, @PathVariable UUID ruleId,
                                @Valid @RequestBody RuleRequest request) {
        return RuleResponse.from(ruleService.update(tenantId(tenantIdHeader), ruleId, request));
    }

    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@RequestHeader("X-Tenant-Id") String tenantIdHeader, @PathVariable UUID ruleId) {
        ruleService.delete(tenantId(tenantIdHeader), ruleId);
        return ResponseEntity.noContent().build();
    }

    private UUID tenantId(String header) {
        if (header == null || header.isBlank()) {
            throw new MissingTenantException();
        }
        return UUID.fromString(header);
    }
}
