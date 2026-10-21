package com.webhookpilot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class EventResponse {

    private UUID id;
    private String eventType;
    private Map<String, Object> payload;
    private String idempotencyKey;
    private Instant createdAt;
    private List<DeliveryAttemptResponse> deliveryAttempts;
}
