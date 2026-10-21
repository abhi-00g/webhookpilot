package com.webhookpilot.dto.request;

import com.webhookpilot.model.enums.EndpointStatus;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class UpdateEndpointRequest {

    @URL(message = "Must be a valid URL")
    private String url;

    private String description;

    private EndpointStatus status;
}
