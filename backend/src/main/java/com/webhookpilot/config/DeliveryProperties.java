package com.webhookpilot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Binds values from application.yml under "app.delivery" prefix.
 * Controls retry behavior, timeouts, and polling interval.
 */
@Component
@ConfigurationProperties(prefix = "app.delivery")
@Getter @Setter
public class DeliveryProperties {

    private int maxAttempts = 6;
    private List<Integer> backoffSeconds = List.of(10, 30, 60, 300, 900);
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 30;
    private long retryPollIntervalMs = 10000;

    /**
     * Calculates when the next retry should happen based on the current attempt number.
     * Returns null if all retries are exhausted.
     *
     * Attempt 1 (initial) fails → next retry in 10s
     * Attempt 2 fails → next retry in 30s
     * Attempt 3 fails → next retry in 60s
     * Attempt 4 fails → next retry in 300s (5 min)
     * Attempt 5 fails → next retry in 900s (15 min)
     * Attempt 6 fails → exhausted, returns null
     */
    public Instant calculateNextRetryAt(int currentAttempt) {
        if (currentAttempt >= maxAttempts) {
            return null;  // exhausted
        }
        int index = Math.min(currentAttempt - 1, backoffSeconds.size() - 1);
        return Instant.now().plusSeconds(backoffSeconds.get(index));
    }
}
