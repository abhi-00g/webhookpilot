package com.webhookpilot.model.enums;

public enum DeliveryStatus {
    PENDING,    // queued, not yet attempted
    SUCCESS,    // endpoint returned 2xx
    FAILED,     // endpoint returned non-2xx or request failed, will retry
    EXHAUSTED   // all retries used up, moved to dead letter queue
}
