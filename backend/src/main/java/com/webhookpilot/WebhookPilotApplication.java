package com.webhookpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // enables @Scheduled for RetryScheduler
public class WebhookPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookPilotApplication.class, args);
    }
}
