package com.webhookpilot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.circuit-breaker")
@Getter @Setter
public class CircuitBreakerProperties {

    private int failureThreshold = 5;
    private int cooldownSeconds = 60;
}
