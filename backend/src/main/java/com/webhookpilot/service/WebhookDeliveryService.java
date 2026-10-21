package com.webhookpilot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookpilot.config.DeliveryProperties;
import com.webhookpilot.model.DeliveryAttempt;
import com.webhookpilot.model.Endpoint;
import com.webhookpilot.model.Event;
import com.webhookpilot.model.enums.DeliveryStatus;
import com.webhookpilot.repository.DeliveryAttemptRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class WebhookDeliveryService {

    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final HmacSignatureService hmacService;
    private final DeliveryProducer deliveryProducer;
    private final DeliveryProperties deliveryProperties;
    private final CircuitBreakerService circuitBreaker;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WebhookDeliveryService(DeliveryAttemptRepository deliveryAttemptRepository,
                                   HmacSignatureService hmacService,
                                   DeliveryProducer deliveryProducer,
                                   DeliveryProperties deliveryProperties,
                                   CircuitBreakerService circuitBreaker,
                                   ObjectMapper objectMapper) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.hmacService = hmacService;
        this.deliveryProducer = deliveryProducer;
        this.deliveryProperties = deliveryProperties;
        this.circuitBreaker = circuitBreaker;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(deliveryProperties.getConnectTimeoutSeconds()))
                .build();
    }

    @Transactional
    public void deliver(Event event, Endpoint endpoint, int attemptNumber) {
        // Set MDC for structured logging — every log in this method gets these fields
        MDC.put("eventId", event.getId().toString());
        MDC.put("endpointId", endpoint.getId().toString());
        MDC.put("attemptNumber", String.valueOf(attemptNumber));
        MDC.put("applicationId", event.getApplication().getId().toString());

        try {
            // Circuit breaker check
            if (!circuitBreaker.allowDelivery(endpoint.getId())) {
                log.info("Circuit OPEN: skipping delivery, scheduling retry");
                MDC.put("deliveryStatus", "CIRCUIT_OPEN");

                // Schedule a retry — the circuit will eventually close
                Instant nextRetry = deliveryProperties.calculateNextRetryAt(attemptNumber);
                if (nextRetry != null) {
                    DeliveryAttempt retryAttempt = DeliveryAttempt.builder()
                            .event(event)
                            .endpoint(endpoint)
                            .attemptNumber(attemptNumber + 1)
                            .status(DeliveryStatus.PENDING)
                            .nextRetryAt(nextRetry)
                            .errorMessage("Circuit breaker open")
                            .build();
                    deliveryAttemptRepository.saveAndFlush(retryAttempt);
                }
                return;
            }

            log.info("Delivering webhook");

            DeliveryAttempt attempt = DeliveryAttempt.builder()
                    .event(event)
                    .endpoint(endpoint)
                    .attemptNumber(attemptNumber)
                    .status(DeliveryStatus.PENDING)
                    .build();
            attempt = deliveryAttemptRepository.saveAndFlush(attempt);

            try {
                String payloadJson = objectMapper.writeValueAsString(event.getPayload());
                String timestamp = String.valueOf(Instant.now().getEpochSecond());
                String signatureContent = timestamp + "." + payloadJson;
                String signature = hmacService.sign(signatureContent, endpoint.getSecret());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint.getUrl()))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "WebhookPilot/1.0")
                        .header("X-Webhook-Id", event.getId().toString())
                        .header("X-Webhook-Timestamp", timestamp)
                        .header("X-Webhook-Signature", "sha256=" + signature)
                        .timeout(Duration.ofSeconds(deliveryProperties.getReadTimeoutSeconds()))
                        .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                attempt.setHttpStatusCode(response.statusCode());
                attempt.setResponseBody(truncate(response.body(), 1024));

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    attempt.setStatus(DeliveryStatus.SUCCESS);
                    MDC.put("deliveryStatus", "SUCCESS");
                    circuitBreaker.recordSuccess(endpoint);
                    log.info("Delivery succeeded with status {}", response.statusCode());
                } else {
                    MDC.put("deliveryStatus", "FAILED");
                    circuitBreaker.recordFailure(endpoint);
                    handleFailure(attempt, event, endpoint, attemptNumber,
                            "HTTP " + response.statusCode());
                }

            } catch (JsonProcessingException e) {
                attempt.setErrorMessage("Failed to serialize payload: " + e.getMessage());
                attempt.setStatus(DeliveryStatus.EXHAUSTED);
                MDC.put("deliveryStatus", "SERIALIZE_ERROR");
                log.error("Payload serialization failed", e);

            } catch (Exception e) {
                MDC.put("deliveryStatus", "FAILED");
                circuitBreaker.recordFailure(endpoint);
                handleFailure(attempt, event, endpoint, attemptNumber, e.getMessage());
            }

            deliveryAttemptRepository.saveAndFlush(attempt);

        } finally {
            MDC.clear();
        }
    }

    @Transactional
    public void retryDelivery(DeliveryAttempt pendingAttempt) {
        Event event = pendingAttempt.getEvent();
        Endpoint endpoint = pendingAttempt.getEndpoint();
        int attemptNumber = pendingAttempt.getAttemptNumber();

        MDC.put("eventId", event.getId().toString());
        MDC.put("endpointId", endpoint.getId().toString());
        MDC.put("attemptNumber", String.valueOf(attemptNumber));

        try {
            // Circuit breaker check on retry too
            if (!circuitBreaker.allowDelivery(endpoint.getId())) {
                log.info("Circuit OPEN on retry: rescheduling");
                Instant nextRetry = deliveryProperties.calculateNextRetryAt(attemptNumber);
                if (nextRetry != null) {
                    pendingAttempt.setNextRetryAt(nextRetry);
                    pendingAttempt.setErrorMessage("Circuit breaker open");
                } else {
                    pendingAttempt.setStatus(DeliveryStatus.EXHAUSTED);
                    pendingAttempt.setNextRetryAt(null);
                    deliveryProducer.publishToDlq(event.getId(), endpoint.getId(),
                            attemptNumber, "Circuit breaker open — exhausted");
                }
                deliveryAttemptRepository.saveAndFlush(pendingAttempt);
                return;
            }

            log.info("Retrying delivery");

            String payloadJson = objectMapper.writeValueAsString(event.getPayload());
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String signatureContent = timestamp + "." + payloadJson;
            String signature = hmacService.sign(signatureContent, endpoint.getSecret());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint.getUrl()))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "WebhookPilot/1.0")
                    .header("X-Webhook-Id", event.getId().toString())
                    .header("X-Webhook-Timestamp", timestamp)
                    .header("X-Webhook-Signature", "sha256=" + signature)
                    .timeout(Duration.ofSeconds(deliveryProperties.getReadTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            pendingAttempt.setHttpStatusCode(response.statusCode());
            pendingAttempt.setResponseBody(truncate(response.body(), 1024));
            pendingAttempt.setNextRetryAt(null);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                pendingAttempt.setStatus(DeliveryStatus.SUCCESS);
                MDC.put("deliveryStatus", "SUCCESS");
                circuitBreaker.recordSuccess(endpoint);
                log.info("Retry succeeded with status {}", response.statusCode());
            } else {
                MDC.put("deliveryStatus", "FAILED");
                circuitBreaker.recordFailure(endpoint);
                scheduleNextRetryOrExhaust(pendingAttempt, event, endpoint, attemptNumber,
                        "HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            pendingAttempt.setNextRetryAt(null);
            circuitBreaker.recordFailure(endpoint);
            scheduleNextRetryOrExhaust(pendingAttempt, event, endpoint, attemptNumber,
                    e.getMessage());
        } finally {
            MDC.clear();
        }

        deliveryAttemptRepository.saveAndFlush(pendingAttempt);
    }

    private void handleFailure(DeliveryAttempt attempt, Event event, Endpoint endpoint,
                                int attemptNumber, String errorMessage) {
        attempt.setErrorMessage(errorMessage);

        Instant nextRetry = deliveryProperties.calculateNextRetryAt(attemptNumber);
        if (nextRetry != null) {
            attempt.setStatus(DeliveryStatus.FAILED);
            log.warn("Delivery failed: {}. Retry scheduled.", errorMessage);

            DeliveryAttempt retryAttempt = DeliveryAttempt.builder()
                    .event(event)
                    .endpoint(endpoint)
                    .attemptNumber(attemptNumber + 1)
                    .status(DeliveryStatus.PENDING)
                    .nextRetryAt(nextRetry)
                    .build();
            deliveryAttemptRepository.saveAndFlush(retryAttempt);
        } else {
            attempt.setStatus(DeliveryStatus.EXHAUSTED);
            deliveryProducer.publishToDlq(event.getId(), endpoint.getId(),
                    attemptNumber, errorMessage);
            log.error("Delivery exhausted after {} attempts", attemptNumber);
        }
    }

    private void scheduleNextRetryOrExhaust(DeliveryAttempt attempt, Event event,
                                             Endpoint endpoint, int attemptNumber,
                                             String errorMessage) {
        attempt.setErrorMessage(errorMessage);

        Instant nextRetry = deliveryProperties.calculateNextRetryAt(attemptNumber);
        if (nextRetry != null) {
            attempt.setStatus(DeliveryStatus.PENDING);
            attempt.setNextRetryAt(nextRetry);
            log.warn("Retry failed: {}. Next retry scheduled.", errorMessage);
        } else {
            attempt.setStatus(DeliveryStatus.EXHAUSTED);
            deliveryProducer.publishToDlq(event.getId(), endpoint.getId(),
                    attemptNumber, errorMessage);
            log.error("Retry exhausted after {} attempts", attemptNumber);
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
