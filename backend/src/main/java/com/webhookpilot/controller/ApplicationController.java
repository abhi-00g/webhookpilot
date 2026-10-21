package com.webhookpilot.controller;

import com.webhookpilot.dto.request.CreateApplicationRequest;
import com.webhookpilot.dto.response.ApplicationResponse;
import com.webhookpilot.service.ApplicationService;
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
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateApplicationRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.create(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(applicationService.listByUser(userId));
    }

    @GetMapping("/{appId}")
    public ResponseEntity<ApplicationResponse> get(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(applicationService.getByIdAndUser(appId, userId));
    }

    @DeleteMapping("/{appId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID appId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        applicationService.delete(appId, userId);
        return ResponseEntity.noContent().build();
    }
}
