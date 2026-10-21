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
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

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

            MDC.put("eventId", msg.getEventId().toString());
            try {
                log.info("Consumed event from Kafka");

                if (deliveryAttemptRepository.existsByEventId(msg.getEventId())) {
                    log.info("Duplicate Kafka delivery, skipping");
                    return;
                }

                Event event = eventRepository.findById(msg.getEventId()).orElse(null);
                if (event == null) {
                    log.warn("Event not found in database, skipping");
                    return;
                }

                MDC.put("applicationId", event.getApplication().getId().toString());

                List<Endpoint> endpoints = endpointRepository
                        .findBySubscribedEventTypeIdAndStatus(
                                event.getEventType().getId(), EndpointStatus.ACTIVE);

                if (endpoints.isEmpty()) {
                    log.info("No active endpoints subscribed to event type {}",
                            event.getEventType().getName());
                    return;
                }

                log.info("Fanning out to {} endpoints", endpoints.size());

                for (Endpoint endpoint : endpoints) {
                    try {
                        deliveryService.deliver(event, endpoint, 1);
                    } catch (Exception e) {
                        log.error("Failed to deliver to endpoint {}: {}",
                                endpoint.getId(), e.getMessage());
                    }
                }
            } finally {
                MDC.clear();
            }

        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.DLQ_TOPIC, groupId = "webhookpilot-dlq")
    public void handleDeadLetter(String message) {
        log.warn("Dead letter received: {}", message);
    }
}
