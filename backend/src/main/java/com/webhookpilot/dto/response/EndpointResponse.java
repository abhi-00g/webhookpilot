package com.webhookpilot.dto.response;

import com.webhookpilot.model.enums.EndpointStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class EndpointResponse {

    private UUID id;
    private String url;
    private String secret;
    private String description;
    private EndpointStatus status;
    private List<EventTypeResponse> subscribedEventTypes;
    private Instant createdAt;
    private Instant updatedAt;
}
