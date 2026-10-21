package com.webhookpilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class CreateEndpointRequest {

    @NotBlank(message = "URL is required")
    @URL(message = "Must be a valid URL")
    private String url;

    private String description;
}
