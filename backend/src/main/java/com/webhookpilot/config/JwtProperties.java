package com.webhookpilot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds values from application.yml under "app.jwt" prefix.
 * Spring auto-maps app.jwt.secret → this.secret, app.jwt.expiration-ms → this.expirationMs.
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter @Setter
public class JwtProperties {

    private String secret;
    private long expirationMs;
}
