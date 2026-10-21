package com.webhookpilot.repository;

import com.webhookpilot.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByUserId(UUID userId);

    Optional<Application> findByApiKey(String apiKey);

    // Ownership check: find app only if it belongs to this user
    Optional<Application> findByIdAndUserId(UUID id, UUID userId);
}
