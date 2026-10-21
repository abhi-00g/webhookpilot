package com.webhookpilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class IngestEventRequest {

    @NotBlank(message = "eventType is required (e.g., 'order.created')")
    private String eventType;

    @NotNull(message = "payload is required")
    private Map<String, Object> payload;

    // Optional: if provided, duplicate events with the same key are rejected
    private String idempotencyKey;
}
