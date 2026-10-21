package com.webhookpilot.repository;

import com.webhookpilot.model.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {

    List<Endpoint> findByApplicationId(UUID applicationId);

    Optional<Endpoint> findByIdAndApplicationId(UUID id, UUID applicationId);
}
