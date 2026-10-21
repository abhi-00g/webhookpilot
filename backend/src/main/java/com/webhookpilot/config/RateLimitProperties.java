package com.webhookpilot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter @Setter
public class RateLimitProperties {

    private int maxEventsPerMinute = 100;
}
