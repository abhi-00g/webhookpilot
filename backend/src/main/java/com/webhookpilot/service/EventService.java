package com.webhookpilot.service;

import com.webhookpilot.dto.request.IngestEventRequest;
import com.webhookpilot.dto.response.DeliveryAttemptResponse;
import com.webhookpilot.dto.response.EventResponse;
import com.webhookpilot.exception.ResourceNotFoundException;
import com.webhookpilot.model.Application;
import com.webhookpilot.model.DeliveryAttempt;
import com.webhookpilot.model.Event;
import com.webhookpilot.model.EventType;
import com.webhookpilot.repository.ApplicationRepository;
import com.webhookpilot.repository.EventRepository;
import com.webhookpilot.repository.EventTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final EventTypeRepository eventTypeRepository;
    private final ApplicationRepository applicationRepository;
    private final DeliveryProducer deliveryProducer;

    /**
     * Ingests an event using API key authentication.
     * Saves to DB, then publishes to Kafka for asynchronous fan-out delivery.
     */
    @Transactional
    public EventResponse ingest(String apiKey, IngestEventRequest request) {
        Application app = applicationRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));

        // Idempotency check
        if (request.getIdempotencyKey() != null) {
            Optional<Event> existing = eventRepository
                    .findByApplicationIdAndIdempotencyKey(app.getId(), request.getIdempotencyKey());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        EventType eventType = eventTypeRepository
                .findByApplicationIdAndName(app.getId(), request.getEventType())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event type '" + request.getEventType() + "' not found in this application"));

        Event event = Event.builder()
                .application(app)
                .eventType(eventType)
                .payload(request.getPayload())
                .idempotencyKey(request.getIdempotencyKey())
                .build();
        event = eventRepository.saveAndFlush(event);

        // Publish to Kafka — delivery happens asynchronously via DeliveryConsumer
        deliveryProducer.publishEvent(event.getId());
        log.info("Event {} ingested and published to Kafka", event.getId());

        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> listByApplication(UUID appId, UUID userId, int page, int size) {
        return eventRepository.findByApplicationId(appId, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getByIdAndApplication(UUID eventId, UUID appId) {
        Event event = eventRepository.findByIdAndApplicationId(eventId, appId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return toResponse(event);
    }

    private EventResponse toResponse(Event event) {
        List<DeliveryAttemptResponse> attempts = event.getDeliveryAttempts() != null
                ? event.getDeliveryAttempts().stream()
                    .map(this::toAttemptResponse)
                    .toList()
                : Collections.emptyList();

        return EventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType().getName())
                .payload(event.getPayload())
                .idempotencyKey(event.getIdempotencyKey())
                .createdAt(event.getCreatedAt())
                .deliveryAttempts(attempts)
                .build();
    }

    private DeliveryAttemptResponse toAttemptResponse(DeliveryAttempt attempt) {
        return DeliveryAttemptResponse.builder()
                .id(attempt.getId())
                .endpointId(attempt.getEndpoint().getId())
                .attemptNumber(attempt.getAttemptNumber())
                .status(attempt.getStatus())
                .httpStatusCode(attempt.getHttpStatusCode())
                .responseBody(attempt.getResponseBody())
                .errorMessage(attempt.getErrorMessage())
                .nextRetryAt(attempt.getNextRetryAt())
                .createdAt(attempt.getCreatedAt())
                .build();
    }
}
