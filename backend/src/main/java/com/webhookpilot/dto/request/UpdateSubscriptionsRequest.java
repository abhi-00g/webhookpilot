package com.webhookpilot.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class UpdateSubscriptionsRequest {

    // Replaces the entire subscription list for this endpoint.
    // Empty set = unsubscribe from everything.
    @NotNull(message = "eventTypeIds is required (can be empty to unsubscribe all)")
    private Set<UUID> eventTypeIds;
}
