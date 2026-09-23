package com.streampulse.gateway.controller;

import com.streampulse.contracts.error.ErrorResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Target of every route's circuit-breaker fallback URI. Returned when a downstream service is
 * unreachable/timing out, so callers get a fast, well-formed 503 instead of a hang or a raw
 * connection-refused stack trace.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public Mono<ResponseEntity<ErrorResponse>> fallback(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                "Downstream service is temporarily unavailable, please retry shortly",
                exchange.getRequest().getURI().getPath(),
                correlationId == null ? "unknown" : correlationId,
                List.of());
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
