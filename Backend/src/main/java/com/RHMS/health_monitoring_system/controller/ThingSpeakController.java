package com.RHMS.health_monitoring_system.controller;

import com.RHMS.health_monitoring_system.service.ThingSpeakService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/ts")
@Validated
public class ThingSpeakController {

    private final ThingSpeakService tsService;

    public ThingSpeakController(ThingSpeakService tsService) {
        this.tsService = tsService;
    }

    @PostMapping("/update")
    public Mono<ResponseEntity<Object>> updateChannel(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {

        if (!tsService.isAuthorized(authorization)) {
            return Mono.just(ResponseEntity.status(401).body((Object) Map.of("error", "Unauthorized")));
        }

        @SuppressWarnings("unchecked")
        var fields = (Map<String, Object>) body.get("fields");
        var status = (String) body.getOrDefault("status", null);

        if (fields == null || fields.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body((Object) Map.of("error", "fields object required")));
        }

        return tsService.update(fields, status)
                .timeout(Duration.ofSeconds(20)) // Increased timeout to match WebClient configuration
                .map(entryId -> {
                    if (entryId == null || entryId.equals("0")) {
                        return ResponseEntity.status(500)
                                .body((Object) Map.of("error", "ThingSpeak write failed", "entryId", entryId));
                    }
                    return ResponseEntity.ok((Object) Map.of("ok", true, "entryId", entryId));
                })
                .onErrorResume(ex -> {
                    ex.printStackTrace();
                    String errorMessage = ex instanceof java.util.concurrent.TimeoutException 
                        ? "Request timeout - ThingSpeak API is taking too long to respond"
                        : "Internal server error: " + ex.getMessage();
                    return Mono.just(ResponseEntity.status(500)
                            .body((Object) Map.of("error", errorMessage, "details", ex.getClass().getSimpleName())));
                });
    }

    @GetMapping("/feeds")
    public Mono<ResponseEntity<Object>> getFeeds(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "results", defaultValue = "10") @Min(1) @Max(100) int results,
            @RequestParam(value = "field", required = false) Integer field) {

        if (!tsService.isAuthorized(authorization)) {
            return Mono.just(ResponseEntity.status(401).body((Object) Map.of("error", "Unauthorized")));
        }

        return tsService.readFeeds(results, field)
                .timeout(Duration.ofSeconds(20)) // Increased timeout to match WebClient configuration
                .map(resp -> ResponseEntity.ok((Object) resp))
                .onErrorResume(ex -> {
                    ex.printStackTrace();
                    String errorMessage = ex instanceof java.util.concurrent.TimeoutException 
                        ? "Request timeout - ThingSpeak API is taking too long to respond"
                        : "Internal server error: " + ex.getMessage();
                    return Mono.just(ResponseEntity.status(500)
                            .body((Object) Map.of("error", errorMessage, "details", ex.getClass().getSimpleName())));
                });
    }
}
