package com.webhookpilot.repository;

import com.webhookpilot.model.DeliveryAttempt;
import com.webhookpilot.model.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    List<DeliveryAttempt> findByEventIdOrderByAttemptNumberAsc(UUID eventId);

    List<DeliveryAttempt> findByEndpointIdOrderByCreatedAtDesc(UUID endpointId);

    /**
     * RetryScheduler query with JOIN FETCH to eagerly load Event, Endpoint,
     * and EventType — prevents lazy loading errors outside the Hibernate session.
     */
    @Query("SELECT da FROM DeliveryAttempt da " +
           "JOIN FETCH da.event e " +
           "JOIN FETCH e.eventType " +
           "JOIN FETCH e.application " +
           "JOIN FETCH da.endpoint " +
           "WHERE da.status = :status AND da.nextRetryAt < :now")
    List<DeliveryAttempt> findRetryableDue(
            @Param("status") DeliveryStatus status,
            @Param("now") Instant now);

    boolean existsByEventId(UUID eventId);
}