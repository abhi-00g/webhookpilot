package com.webhookpilot.service;

import com.webhookpilot.model.DeliveryAttempt;
import com.webhookpilot.model.enums.DeliveryStatus;
import com.webhookpilot.repository.DeliveryAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Database-backed retry scheduler.
 *
 * Why not Kafka retry topics?
 * Kafka doesn't natively support delayed messages. Some systems create multiple
 * retry topics with different poll intervals (retry.10s, retry.30s, retry.5m),
 * but that's over-engineered for our scale. The database-backed approach gives us:
 * - Precise retry timing (not limited to topic poll intervals)
 * - Full visibility into pending retries (queryable via SQL)
 * - Simpler to implement and explain
 *
 * This is the same pattern Stripe and similar webhook platforms use in production.
 *
 * Runs every 10 seconds (configurable via app.delivery.retry-poll-interval-ms).
 * Finds PENDING DeliveryAttempts whose next_retry_at has passed, and re-attempts delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetryScheduler {

    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final WebhookDeliveryService deliveryService;

    @Scheduled(fixedDelayString = "${app.delivery.retry-poll-interval-ms:10000}")
    public void pollForRetries() {
        List<DeliveryAttempt> dueRetries = deliveryAttemptRepository
                .findRetryableDue(DeliveryStatus.PENDING, Instant.now());

        if (!dueRetries.isEmpty()) {
            log.info("Found {} delivery attempts due for retry", dueRetries.size());
        }

        for (DeliveryAttempt attempt : dueRetries) {
            try {
                deliveryService.retryDelivery(attempt);
            } catch (Exception e) {
                log.error("Retry failed for delivery attempt {}: {}",
                        attempt.getId(), e.getMessage());
            }
        }
    }
}
