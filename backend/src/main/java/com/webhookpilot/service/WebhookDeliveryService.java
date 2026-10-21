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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Core delivery engine: signs the payload, POSTs it to the endpoint, records the result.
 *
 * Each delivery attempt:
 * 1. Serializes the event payload to JSON
 * 2. Computes HMAC-SHA256 signature using the endpoint's secret
 * 3. POSTs to the endpoint URL with webhook headers
 * 4. Records the HTTP status, response body (first 1KB), and any error
 * 5. On failure: schedules a retry with exponential backoff, or marks as EXHAUSTED
 */
@Service
@Slf4j
public class WebhookDeliveryService {

    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final HmacSignatureService hmacService;
    private final DeliveryProducer deliveryProducer;
    private final DeliveryProperties deliveryProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WebhookDeliveryService(DeliveryAttemptRepository deliveryAttemptRepository,
                                   HmacSignatureService hmacService,
                                   DeliveryProducer deliveryProducer,
                                   DeliveryProperties deliveryProperties,
                                   ObjectMapper objectMapper) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.hmacService = hmacService;
        this.deliveryProducer = deliveryProducer;
        this.deliveryProperties = deliveryProperties;
        this.objectMapper = objectMapper;

        // Single reusable HttpClient with connect timeout
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(deliveryProperties.getConnectTimeoutSeconds()))
                .build();
    }

    /**
     * Attempts delivery of an event to an endpoint.
     * Creates a DeliveryAttempt record and updates it based on the HTTP result.
     */
    @Transactional
    public void deliver(Event event, Endpoint endpoint, int attemptNumber) {
        log.info("Delivering event {} to endpoint {} (attempt {})",
                event.getId(), endpoint.getId(), attemptNumber);

        // Create the attempt record
        DeliveryAttempt attempt = DeliveryAttempt.builder()
                .event(event)
                .endpoint(endpoint)
                .attemptNumber(attemptNumber)
                .status(DeliveryStatus.PENDING)
                .build();
        attempt = deliveryAttemptRepository.saveAndFlush(attempt);

        try {
            // Serialize payload
            String payloadJson = objectMapper.writeValueAsString(event.getPayload());
            String timestamp = String.valueOf(Instant.now().getEpochSecond());

            // Sign: HMAC-SHA256(timestamp + "." + payload, secret)
            String signatureContent = timestamp + "." + payloadJson;
            String signature = hmacService.sign(signatureContent, endpoint.getSecret());

            // Build HTTP request with webhook headers
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

            // Send
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            attempt.setHttpStatusCode(response.statusCode());
            attempt.setResponseBody(truncate(response.body(), 1024));

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                attempt.setStatus(DeliveryStatus.SUCCESS);
                log.info("Delivery SUCCESS: event={}, endpoint={}, status={}",
                        event.getId(), endpoint.getId(), response.statusCode());
            } else {
                handleFailure(attempt, event, endpoint, attemptNumber,
                        "HTTP " + response.statusCode());
            }

        } catch (JsonProcessingException e) {
            attempt.setErrorMessage("Failed to serialize payload: " + e.getMessage());
            attempt.setStatus(DeliveryStatus.EXHAUSTED);
            log.error("Payload serialization failed for event {}", event.getId(), e);

        } catch (Exception e) {
            handleFailure(attempt, event, endpoint, attemptNumber, e.getMessage());
        }

        deliveryAttemptRepository.saveAndFlush(attempt);
    }

    /**
     * Retries delivery for an existing PENDING DeliveryAttempt (called by RetryScheduler).
     */
    @Transactional
    public void retryDelivery(DeliveryAttempt pendingAttempt) {
        Event event = pendingAttempt.getEvent();
        Endpoint endpoint = pendingAttempt.getEndpoint();
        int attemptNumber = pendingAttempt.getAttemptNumber();

        log.info("Retrying delivery: event={}, endpoint={}, attempt={}",
                event.getId(), endpoint.getId(), attemptNumber);

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

            pendingAttempt.setHttpStatusCode(response.statusCode());
            pendingAttempt.setResponseBody(truncate(response.body(), 1024));
            pendingAttempt.setNextRetryAt(null);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                pendingAttempt.setStatus(DeliveryStatus.SUCCESS);
                log.info("Retry SUCCESS: event={}, endpoint={}, attempt={}",
                        event.getId(), endpoint.getId(), attemptNumber);
            } else {
                scheduleNextRetryOrExhaust(pendingAttempt, event, endpoint, attemptNumber,
                        "HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            pendingAttempt.setNextRetryAt(null);
            scheduleNextRetryOrExhaust(pendingAttempt, event, endpoint, attemptNumber,
                    e.getMessage());
        }

        deliveryAttemptRepository.saveAndFlush(pendingAttempt);
    }

    /**
     * Handles a failed initial delivery: schedules retry or marks exhausted.
     */
    private void handleFailure(DeliveryAttempt attempt, Event event, Endpoint endpoint,
                                int attemptNumber, String errorMessage) {
        attempt.setErrorMessage(errorMessage);

        Instant nextRetry = deliveryProperties.calculateNextRetryAt(attemptNumber);
        if (nextRetry != null) {
            // Schedule retry: create a new PENDING attempt with next_retry_at
            attempt.setStatus(DeliveryStatus.FAILED);
            log.warn("Delivery FAILED: event={}, endpoint={}, attempt={}, error={}. Scheduling retry.",
                    event.getId(), endpoint.getId(), attemptNumber, errorMessage);

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
            log.error("Delivery EXHAUSTED: event={}, endpoint={}, attempts={}",
                    event.getId(), endpoint.getId(), attemptNumber);
        }
    }

    /**
     * Handles a failed retry: schedules next retry or marks exhausted.
     */
    private void scheduleNextRetryOrExhaust(DeliveryAttempt attempt, Event event,
                                             Endpoint endpoint, int attemptNumber,
                                             String errorMessage) {
        attempt.setErrorMessage(errorMessage);

        Instant nextRetry = deliveryProperties.calculateNextRetryAt(attemptNumber);
        if (nextRetry != null) {
            // Keep this attempt as PENDING with a new next_retry_at
            attempt.setStatus(DeliveryStatus.PENDING);
            attempt.setNextRetryAt(nextRetry);
            log.warn("Retry FAILED: event={}, endpoint={}, attempt={}. Next retry at {}",
                    event.getId(), endpoint.getId(), attemptNumber, nextRetry);
        } else {
            attempt.setStatus(DeliveryStatus.EXHAUSTED);
            deliveryProducer.publishToDlq(event.getId(), endpoint.getId(),
                    attemptNumber, errorMessage);
            log.error("Retry EXHAUSTED: event={}, endpoint={}, attempts={}",
                    event.getId(), endpoint.getId(), attemptNumber);
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
