package com.webhookpilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateEventTypeRequest {

    @NotBlank(message = "Event type name is required")
    @Pattern(regexp = "^[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9]*)+$",
             message = "Must use dotted lowercase format (e.g., 'order.created', 'payment.failed')")
    private String name;

    private String description;
}
