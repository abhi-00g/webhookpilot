package com.webhookpilot.repository;

import com.webhookpilot.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByApplicationId(UUID applicationId, Pageable pageable);

    Optional<Event> findByIdAndApplicationId(UUID id, UUID applicationId);

    // Idempotency check: find existing event with the same key in the same app
    Optional<Event> findByApplicationIdAndIdempotencyKey(UUID applicationId, String idempotencyKey);
}
