package com.webhookpilot.repository;

import com.webhookpilot.model.Endpoint;
import com.webhookpilot.model.enums.EndpointStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {

    List<Endpoint> findByApplicationId(UUID applicationId);

    Optional<Endpoint> findByIdAndApplicationId(UUID id, UUID applicationId);

    /**
     * Fan-out query: find all ACTIVE endpoints that subscribe to a given event type.
     * This is the core of webhook delivery — one event fans out to N endpoints.
     */
    @Query("SELECT e FROM Endpoint e JOIN e.subscribedEventTypes et " +
           "WHERE et.id = :eventTypeId AND e.status = :status")
    List<Endpoint> findBySubscribedEventTypeIdAndStatus(
            @Param("eventTypeId") UUID eventTypeId,
            @Param("status") EndpointStatus status);
}
