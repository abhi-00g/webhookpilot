package com.webhookpilot.service;

import com.webhookpilot.dto.request.CreateEventTypeRequest;
import com.webhookpilot.dto.response.EventTypeResponse;
import com.webhookpilot.exception.DuplicateResourceException;
import com.webhookpilot.exception.ResourceNotFoundException;
import com.webhookpilot.model.Application;
import com.webhookpilot.model.EventType;
import com.webhookpilot.repository.EventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventTypeService {

    private final EventTypeRepository eventTypeRepository;
    private final ApplicationService applicationService;

    @Transactional
    public EventTypeResponse create(UUID appId, UUID userId, CreateEventTypeRequest request) {
        Application app = applicationService.findByIdAndUser(appId, userId);

        if (eventTypeRepository.existsByApplicationIdAndName(appId, request.getName())) {
            throw new DuplicateResourceException(
                    "Event type '" + request.getName() + "' already exists in this application");
        }

        EventType eventType = EventType.builder()
                .application(app)
                .name(request.getName())
                .description(request.getDescription())
                .build();
        eventType = eventTypeRepository.saveAndFlush(eventType);

        return toResponse(eventType);
    }

    @Transactional(readOnly = true)
    public List<EventTypeResponse> listByApplication(UUID appId, UUID userId) {
        applicationService.findByIdAndUser(appId, userId);
        return eventTypeRepository.findByApplicationId(appId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID eventTypeId, UUID appId, UUID userId) {
        applicationService.findByIdAndUser(appId, userId);
        EventType eventType = eventTypeRepository.findByIdAndApplicationId(eventTypeId, appId)
                .orElseThrow(() -> new ResourceNotFoundException("Event type not found"));
        eventTypeRepository.delete(eventType);
    }

    public EventTypeResponse toResponse(EventType et) {
        return EventTypeResponse.builder()
                .id(et.getId())
                .name(et.getName())
                .description(et.getDescription())
                .createdAt(et.getCreatedAt())
                .build();
    }
}
