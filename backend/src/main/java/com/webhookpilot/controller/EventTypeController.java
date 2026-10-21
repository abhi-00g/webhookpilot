package com.webhookpilot.controller;

import com.webhookpilot.dto.request.CreateEventTypeRequest;
import com.webhookpilot.dto.response.EventTypeResponse;
import com.webhookpilot.service.EventTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications/{appId}/event-types")
@RequiredArgsConstructor
public class EventTypeController {

    private final EventTypeService eventTypeService;

    @PostMapping
    public ResponseEntity<EventTypeResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId,
            @Valid @RequestBody CreateEventTypeRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventTypeService.create(appId, userId, request));
    }

    @GetMapping
    public ResponseEntity<List<EventTypeResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(eventTypeService.listByApplication(appId, userId));
    }

    @DeleteMapping("/{eventTypeId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId,
            @PathVariable UUID eventTypeId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        eventTypeService.delete(eventTypeId, appId, userId);
        return ResponseEntity.noContent().build();
    }
}
