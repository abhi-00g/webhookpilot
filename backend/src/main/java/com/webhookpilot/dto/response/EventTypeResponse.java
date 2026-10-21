package com.webhookpilot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class EventTypeResponse {

    private UUID id;
    private String name;
    private String description;
    private Instant createdAt;
}
