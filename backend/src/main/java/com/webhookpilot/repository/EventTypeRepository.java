package com.webhookpilot.repository;

import com.webhookpilot.model.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventTypeRepository extends JpaRepository<EventType, UUID> {

    List<EventType> findByApplicationId(UUID applicationId);

    Optional<EventType> findByApplicationIdAndName(UUID applicationId, String name);

    Optional<EventType> findByIdAndApplicationId(UUID id, UUID applicationId);

    boolean existsByApplicationIdAndName(UUID applicationId, String name);
}
