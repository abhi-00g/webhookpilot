package com.webhookpilot.service;

import com.webhookpilot.dto.request.CreateEndpointRequest;
import com.webhookpilot.dto.request.UpdateEndpointRequest;
import com.webhookpilot.dto.request.UpdateSubscriptionsRequest;
import com.webhookpilot.dto.response.EndpointResponse;
import com.webhookpilot.dto.response.EventTypeResponse;
import com.webhookpilot.exception.ResourceNotFoundException;
import com.webhookpilot.model.Application;
import com.webhookpilot.model.Endpoint;
import com.webhookpilot.model.EventType;
import com.webhookpilot.repository.EndpointRepository;
import com.webhookpilot.repository.EventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EndpointService {

    private final EndpointRepository endpointRepository;
    private final EventTypeRepository eventTypeRepository;
    private final ApplicationService applicationService;
    private final EventTypeService eventTypeService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public EndpointResponse create(UUID appId, UUID userId, CreateEndpointRequest request) {
        Application app = applicationService.findByIdAndUser(appId, userId);

        Endpoint endpoint = Endpoint.builder()
                .application(app)
                .url(request.getUrl())
                .secret(generateSigningSecret())
                .description(request.getDescription())
                .build();
        endpoint = endpointRepository.saveAndFlush(endpoint);

        return toResponse(endpoint);
    }

    @Transactional(readOnly = true)
    public List<EndpointResponse> listByApplication(UUID appId, UUID userId) {
        applicationService.findByIdAndUser(appId, userId);
        return endpointRepository.findByApplicationId(appId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EndpointResponse update(UUID endpointId, UUID appId, UUID userId, UpdateEndpointRequest request) {
        applicationService.findByIdAndUser(appId, userId);
        Endpoint endpoint = findByIdAndApp(endpointId, appId);

        if (request.getUrl() != null) {
            endpoint.setUrl(request.getUrl());
        }
        if (request.getDescription() != null) {
            endpoint.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            endpoint.setStatus(request.getStatus());
        }

        endpoint = endpointRepository.saveAndFlush(endpoint);
        return toResponse(endpoint);
    }

    @Transactional
    public void delete(UUID endpointId, UUID appId, UUID userId) {
        applicationService.findByIdAndUser(appId, userId);
        Endpoint endpoint = findByIdAndApp(endpointId, appId);
        endpointRepository.delete(endpoint);
    }

    @Transactional
    public EndpointResponse updateSubscriptions(UUID endpointId, UUID appId, UUID userId,
                                                 UpdateSubscriptionsRequest request) {
        applicationService.findByIdAndUser(appId, userId);
        Endpoint endpoint = findByIdAndApp(endpointId, appId);

        Set<EventType> eventTypes = new HashSet<>();
        for (UUID eventTypeId : request.getEventTypeIds()) {
            EventType et = eventTypeRepository.findByIdAndApplicationId(eventTypeId, appId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Event type " + eventTypeId + " not found in this application"));
            eventTypes.add(et);
        }

        endpoint.setSubscribedEventTypes(eventTypes);
        endpoint = endpointRepository.saveAndFlush(endpoint);
        return toResponse(endpoint);
    }

    @Transactional(readOnly = true)
    public List<EventTypeResponse> getSubscriptions(UUID endpointId, UUID appId, UUID userId) {
        applicationService.findByIdAndUser(appId, userId);
        Endpoint endpoint = findByIdAndApp(endpointId, appId);

        return endpoint.getSubscribedEventTypes().stream()
                .map(eventTypeService::toResponse)
                .toList();
    }

    private Endpoint findByIdAndApp(UUID endpointId, UUID appId) {
        return endpointRepository.findByIdAndApplicationId(endpointId, appId)
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint not found"));
    }

    private String generateSigningSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder("whsec_");
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private EndpointResponse toResponse(Endpoint endpoint) {
        List<EventTypeResponse> subscriptions = endpoint.getSubscribedEventTypes().stream()
                .map(eventTypeService::toResponse)
                .collect(Collectors.toList());

        return EndpointResponse.builder()
                .id(endpoint.getId())
                .url(endpoint.getUrl())
                .secret(endpoint.getSecret())
                .description(endpoint.getDescription())
                .status(endpoint.getStatus())
                .subscribedEventTypes(subscriptions)
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .build();
    }
}
