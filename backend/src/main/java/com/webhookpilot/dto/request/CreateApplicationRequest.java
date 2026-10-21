package com.webhookpilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateApplicationRequest {

    @NotBlank(message = "Application name is required")
    @Size(max = 100, message = "Name must be 100 characters or less")
    private String name;
}
