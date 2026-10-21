package com.webhookpilot.dto.response;

import com.webhookpilot.model.enums.DeliveryStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DeliveryAttemptResponse {

    private UUID id;
    private UUID endpointId;
    private int attemptNumber;
    private DeliveryStatus status;
    private Integer httpStatusCode;
    private String responseBody;
    private String errorMessage;
    private Instant nextRetryAt;
    private Instant createdAt;
}
