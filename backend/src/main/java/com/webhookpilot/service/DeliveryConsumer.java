package com.webhookpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookpilot.config.KafkaTopicConfig;
import com.webhookpilot.dto.kafka.WebhookDeliveryMessage;
import com.webhookpilot.model.Endpoint;
import com.webhookpilot.model.Event;
import com.webhookpilot.model.enums.EndpointStatus;
import com.webhookpilot.repository.DeliveryAttemptRepository;
import com.webhookpilot.repository.EndpointRepository;
import com.webhookpilot.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Kafka consumer for the main webhook.events topic.
 *
 * Flow:
 * 1. Receives a message containing an eventId
 * 2. Looks up the event from the database
 * 3. Finds all ACTIVE endpoints subscribed to this event's type (fan-out)
 * 4. Delivers the event to each endpoint via WebhookDeliveryService
 *
 * Idempotency: checks if delivery attempts already exist for this event
 * to prevent duplicate fan-out from Kafka's at-least-once delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryConsumer {

    private final EventRepository eventRepository;
    private final EndpointRepository endpointRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final WebhookDeliveryService deliveryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopicConfig.EVENTS_TOPIC, groupId = "webhookpilot-delivery")
    public void handleEvent(String message) {
        try {
            WebhookDeliveryMessage msg = objectMapper.readValue(message, WebhookDeliveryMessage.class);
            log.info("Consumed event {} from {}", msg.getEventId(), KafkaTopicConfig.EVENTS_TOPIC);

            // Idempotency: skip if already processed (duplicate Kafka delivery)
            if (deliveryAttemptRepository.existsByEventId(msg.getEventId())) {
                log.info("Event {} already has delivery attempts, skipping duplicate", msg.getEventId());
                return;
            }

            // Look up the event
            Event event = eventRepository.findById(msg.getEventId()).orElse(null);
            if (event == null) {
                log.warn("Event {} not found in database, skipping", msg.getEventId());
                return;
            }

            // Fan-out: find all active endpoints subscribed to this event type
            List<Endpoint> endpoints = endpointRepository
                    .findBySubscribedEventTypeIdAndStatus(
                            event.getEventType().getId(), EndpointStatus.ACTIVE);

            if (endpoints.isEmpty()) {
                log.info("No active endpoints subscribed to event type {} for event {}",
                        event.getEventType().getName(), event.getId());
                return;
            }

            log.info("Fanning out event {} to {} endpoints", event.getId(), endpoints.size());

            // Deliver to each endpoint
            for (Endpoint endpoint : endpoints) {
                try {
                    deliveryService.deliver(event, endpoint, 1);
                } catch (Exception e) {
                    // Don't let one endpoint failure stop delivery to others
                    log.error("Failed to deliver event {} to endpoint {}: {}",
                            event.getId(), endpoint.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to process delivery message: {}", message, e);
        }
    }

    /**
     * DLQ consumer — logs exhausted deliveries for visibility.
     * In Phase 4, the dashboard will display these and allow manual replay.
     */
    @KafkaListener(topics = KafkaTopicConfig.DLQ_TOPIC, groupId = "webhookpilot-dlq")
    public void handleDeadLetter(String message) {
        log.warn("Dead letter received: {}", message);
    }
}
