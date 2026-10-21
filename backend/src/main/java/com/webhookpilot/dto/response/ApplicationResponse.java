package com.webhookpilot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ApplicationResponse {

    private UUID id;
    private String name;
    private String apiKey;
    private Instant createdAt;
    private Instant updatedAt;
}
