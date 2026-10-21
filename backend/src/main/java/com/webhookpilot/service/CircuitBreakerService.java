package com.webhookpilot.service;

import com.webhookpilot.config.CircuitBreakerProperties;
import com.webhookpilot.model.Endpoint;
import com.webhookpilot.model.enums.EndpointStatus;
import com.webhookpilot.repository.EndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis-backed circuit breaker for webhook endpoints.
 *
 * The circuit breaker protects both sides:
 * - The subscriber's server from being hammered when it's down
 * - Our system from wasting resources on endpoints that won't respond
 *
 * Redis keys per endpoint:
 *   circuit:{endpointId}:failures  → consecutive failure count (no TTL, reset on success)
 *   circuit:{endpointId}:open      → exists = circuit is open (TTL = cooldown period)
 *
 * State transitions:
 *   CLOSED → failures reach threshold → OPEN (endpoint DISABLED, Redis key with TTL)
 *   OPEN   → TTL expires → HALF-OPEN (key gone, next attempt is a probe)
 *   HALF-OPEN → probe succeeds → CLOSED (endpoint ACTIVE, counter reset)
 *   HALF-OPEN → probe fails → OPEN (TTL reset)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerService {

    private final StringRedisTemplate redisTemplate;
    private final EndpointRepository endpointRepository;
    private final CircuitBreakerProperties properties;

    private static final String FAILURE_KEY_PREFIX = "circuit:%s:failures";
    private static final String OPEN_KEY_PREFIX = "circuit:%s:open";

    /**
     * Checks if delivery should be attempted for this endpoint.
     * Returns true if the circuit is CLOSED or HALF-OPEN (probe allowed).
     * Returns false if the circuit is OPEN (skip delivery).
     */
    public boolean allowDelivery(UUID endpointId) {
        String openKey = String.format(OPEN_KEY_PREFIX, endpointId);
        Boolean isOpen = redisTemplate.hasKey(openKey);
        return !Boolean.TRUE.equals(isOpen);
    }

    /**
     * Called after a successful delivery. Resets the failure counter
     * and re-enables the endpoint if it was DISABLED by the circuit breaker.
     */
    public void recordSuccess(Endpoint endpoint) {
        String failureKey = String.format(FAILURE_KEY_PREFIX, endpoint.getId());
        redisTemplate.delete(failureKey);

        // Re-enable endpoint if it was disabled by circuit breaker
        if (endpoint.getStatus() == EndpointStatus.DISABLED) {
            endpoint.setStatus(EndpointStatus.ACTIVE);
            endpointRepository.saveAndFlush(endpoint);
            log.info("Circuit CLOSED: endpoint {} re-enabled after successful probe",
                    endpoint.getId());
        }
    }

    /**
     * Called after a failed delivery. Increments the failure counter.
     * If the threshold is reached, opens the circuit and disables the endpoint.
     */
    public void recordFailure(Endpoint endpoint) {
        String failureKey = String.format(FAILURE_KEY_PREFIX, endpoint.getId());
        String openKey = String.format(OPEN_KEY_PREFIX, endpoint.getId());

        // Increment consecutive failure count
        Long failures = redisTemplate.opsForValue().increment(failureKey);
        if (failures == null) return;

        if (failures >= properties.getFailureThreshold()) {
            // Open the circuit: set a key with TTL (cooldown period)
            redisTemplate.opsForValue().set(openKey, "open",
                    Duration.ofSeconds(properties.getCooldownSeconds()));

            // Disable the endpoint in the database
            if (endpoint.getStatus() != EndpointStatus.DISABLED) {
                endpoint.setStatus(EndpointStatus.DISABLED);
                endpointRepository.saveAndFlush(endpoint);
                log.warn("Circuit OPEN: endpoint {} disabled after {} consecutive failures. " +
                         "Cooldown: {}s",
                        endpoint.getId(), failures, properties.getCooldownSeconds());
            }
        }
    }

    /**
     * Resets the circuit breaker for an endpoint (e.g., when manually re-enabled via API).
     */
    public void reset(UUID endpointId) {
        String failureKey = String.format(FAILURE_KEY_PREFIX, endpointId);
        String openKey = String.format(OPEN_KEY_PREFIX, endpointId);
        redisTemplate.delete(failureKey);
        redisTemplate.delete(openKey);
        log.info("Circuit breaker RESET for endpoint {}", endpointId);
    }
}
