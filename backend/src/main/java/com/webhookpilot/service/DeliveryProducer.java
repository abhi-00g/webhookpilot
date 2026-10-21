package com.webhookpilot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookpilot.config.KafkaTopicConfig;
import com.webhookpilot.dto.kafka.WebhookDeliveryMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Publishes events to Kafka topics.
 *
 * Main topic (webhook.events): triggers fan-out delivery to all matching endpoints.
 * DLQ topic (webhook.events.dlq): receives events that exhausted all retry attempts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes an event to the main delivery topic.
     * Uses the eventId as the Kafka key — this ensures all messages for the same event
     * land in the same partition, preserving ordering per event.
     */
    public void publishEvent(UUID eventId) {
        WebhookDeliveryMessage message = new WebhookDeliveryMessage(eventId);
        try {
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(KafkaTopicConfig.EVENTS_TOPIC, eventId.toString(), json);
            log.info("Published event {} to {}", eventId, KafkaTopicConfig.EVENTS_TOPIC);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize delivery message for event {}", eventId, e);
        }
    }

    /**
     * Publishes an exhausted delivery to the dead letter queue.
     * Includes event and endpoint context for debugging and potential manual replay.
     */
    public void publishToDlq(UUID eventId, UUID endpointId, int attemptNumber, String lastError) {
        try {
            String json = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "eventId", eventId.toString(),
                            "endpointId", endpointId.toString(),
                            "attemptNumber", attemptNumber,
                            "error", lastError != null ? lastError : "unknown",
                            "exhaustedAt", java.time.Instant.now().toString()
                    ));
            kafkaTemplate.send(KafkaTopicConfig.DLQ_TOPIC, eventId.toString(), json);
            log.warn("Published to DLQ: event={}, endpoint={}, attempts={}",
                    eventId, endpointId, attemptNumber);
        } catch (JsonProcessingException e) {
            log.error("Failed to publish to DLQ for event {}", eventId, e);
        }
    }
}
