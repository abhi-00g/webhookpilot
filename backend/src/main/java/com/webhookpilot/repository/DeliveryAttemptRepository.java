package com.webhookpilot.repository;

import com.webhookpilot.model.DeliveryAttempt;
import com.webhookpilot.model.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    List<DeliveryAttempt> findByEventIdOrderByAttemptNumberAsc(UUID eventId);

    List<DeliveryAttempt> findByEndpointIdOrderByCreatedAtDesc(UUID endpointId);

    /**
     * RetryScheduler query: find PENDING delivery attempts that are due for retry.
     * PENDING + nextRetryAt means "scheduled for retry at this time".
     */
    List<DeliveryAttempt> findByStatusAndNextRetryAtBefore(DeliveryStatus status, Instant now);

    /**
     * Idempotency check: prevent duplicate fan-out if Kafka delivers the same message twice.
     */
    boolean existsByEventId(UUID eventId);
}
