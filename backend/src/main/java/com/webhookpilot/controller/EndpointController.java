package com.webhookpilot.controller;

import com.webhookpilot.dto.request.CreateEndpointRequest;
import com.webhookpilot.dto.request.UpdateEndpointRequest;
import com.webhookpilot.dto.request.UpdateSubscriptionsRequest;
import com.webhookpilot.dto.response.EndpointResponse;
import com.webhookpilot.dto.response.EventTypeResponse;
import com.webhookpilot.service.EndpointService;
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
@RequestMapping("/api/applications/{appId}/endpoints")
@RequiredArgsConstructor
public class EndpointController {

    private final EndpointService endpointService;

    @PostMapping
    public ResponseEntity<EndpointResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId,
            @Valid @RequestBody CreateEndpointRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(endpointService.create(appId, userId, request));
    }

    @GetMapping
    public ResponseEntity<List<EndpointResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(endpointService.listByApplication(appId, userId));
    }

    @PatchMapping("/{endpointId}")
    public ResponseEntity<EndpointResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId,
            @PathVariable UUID endpointId,
            @Valid @RequestBody UpdateEndpointRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(endpointService.update(endpointId, appId, userId, request));
    }

    @DeleteMapping("/{endpointId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId,
            @PathVariable UUID endpointId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        endpointService.delete(endpointId, appId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Replace the full subscription list for this endpoint.
     * Body: { "eventTypeIds": ["uuid1", "uuid2"] }
     */
    @PutMapping("/{endpointId}/event-types")
    public ResponseEntity<EndpointResponse> updateSubscriptions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId,
            @PathVariable UUID endpointId,
            @Valid @RequestBody UpdateSubscriptionsRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(
                endpointService.updateSubscriptions(endpointId, appId, userId, request));
    }

    @GetMapping("/{endpointId}/event-types")
    public ResponseEntity<List<EventTypeResponse>> getSubscriptions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId,
            @PathVariable UUID endpointId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(
                endpointService.getSubscriptions(endpointId, appId, userId));
    }
}
