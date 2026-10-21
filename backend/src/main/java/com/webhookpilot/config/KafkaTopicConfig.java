package com.webhookpilot.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Auto-creates Kafka topics on application startup.
 * Spring's KafkaAdmin bean detects these NewTopic beans and creates them if they don't exist.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String EVENTS_TOPIC = "webhook.events";
    public static final String DLQ_TOPIC = "webhook.events.dlq";

    @Bean
    public NewTopic eventsTopic() {
        return TopicBuilder.name(EVENTS_TOPIC)
                .partitions(3)     // 3 partitions for parallel consumption
                .replicas(1)       // 1 replica for local dev (production would use 3)
                .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(DLQ_TOPIC)
                .partitions(1)     // DLQ is low-volume, 1 partition is fine
                .replicas(1)
                .build();
    }
}
