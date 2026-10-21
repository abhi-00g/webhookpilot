package com.webhookpilot.controller;

import com.webhookpilot.dto.request.IngestEventRequest;
import com.webhookpilot.dto.response.EventResponse;
import com.webhookpilot.service.ApplicationService;
import com.webhookpilot.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final ApplicationService applicationService;

    /**
     * Public endpoint — authenticated via X-API-Key header, not JWT.
     * This is how external applications send events to WebhookPilot.
     *
     * Example:
     *   POST /api/events/ingest
     *   X-API-Key: whp_a1b2c3d4...
     *   Body: { "eventType": "order.created", "payload": {...} }
     */
    @PostMapping("/api/events/ingest")
    public ResponseEntity<EventResponse> ingest(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody IngestEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.ingest(apiKey, request));
    }

    /**
     * List events for an application (JWT-protected).
     * Supports pagination via ?page=0&size=20.
     */
    @GetMapping("/api/applications/{appId}/events")
    public ResponseEntity<Page<EventResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        // Verify ownership before listing
        applicationService.findByIdAndUser(appId, userId);
        return ResponseEntity.ok(eventService.listByApplication(appId, userId, page, size));
    }

    /**
     * Get a single event with its delivery attempts (JWT-protected).
     */
    @GetMapping("/api/applications/{appId}/events/{eventId}")
    public ResponseEntity<EventResponse> get(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId,
            @PathVariable UUID eventId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        applicationService.findByIdAndUser(appId, userId);
        return ResponseEntity.ok(eventService.getByIdAndApplication(eventId, appId));
    }
}
