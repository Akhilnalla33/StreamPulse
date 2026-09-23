package com.streampulse.ingestion.controller;

import com.streampulse.ingestion.dto.IngestEventRequest;
import com.streampulse.ingestion.dto.IngestEventResponse;
import com.streampulse.ingestion.exception.MissingTenantException;
import com.streampulse.ingestion.service.EventIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventIngestionService ingestionService;

    public EventController(EventIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public ResponseEntity<IngestEventResponse> ingest(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @Valid @RequestBody IngestEventRequest request) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new MissingTenantException();
        }
        String eventId = ingestionService.ingest(tenantId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(IngestEventResponse.accepted(eventId));
    }
}
