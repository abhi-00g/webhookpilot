package com.webhookpilot.service;

import com.webhookpilot.dto.request.CreateApplicationRequest;
import com.webhookpilot.dto.response.ApplicationResponse;
import com.webhookpilot.exception.ResourceNotFoundException;
import com.webhookpilot.model.Application;
import com.webhookpilot.model.User;
import com.webhookpilot.repository.ApplicationRepository;
import com.webhookpilot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApplicationResponse create(UUID userId, CreateApplicationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Application app = Application.builder()
                .user(user)
                .name(request.getName())
                .apiKey(generateApiKey())
                .build();
        app = applicationRepository.saveAndFlush(app);

        return toResponse(app);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listByUser(UUID userId) {
        return applicationRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getByIdAndUser(UUID appId, UUID userId) {
        Application app = findByIdAndUser(appId, userId);
        return toResponse(app);
    }

    @Transactional
    public void delete(UUID appId, UUID userId) {
        Application app = findByIdAndUser(appId, userId);
        applicationRepository.delete(app);
    }

    public Application findByIdAndUser(UUID appId, UUID userId) {
        return applicationRepository.findByIdAndUserId(appId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private String generateApiKey() {
        return "whp_" + UUID.randomUUID().toString().replace("-", "");
    }

    private ApplicationResponse toResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .name(app.getName())
                .apiKey(app.getApiKey())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
