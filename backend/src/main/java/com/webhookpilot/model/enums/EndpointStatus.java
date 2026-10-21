package com.webhookpilot.model.enums;

public enum EndpointStatus {
    ACTIVE,     // receiving deliveries normally
    PAUSED,     // user manually paused — no deliveries sent
    DISABLED    // circuit breaker tripped — auto-disabled (Phase 3)
}
