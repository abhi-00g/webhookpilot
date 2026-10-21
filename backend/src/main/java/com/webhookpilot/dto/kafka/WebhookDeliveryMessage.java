package com.webhookpilot.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Kafka message published when a new event is ingested.
 * The consumer uses the eventId to look up the event and fan out to matching endpoints.
 *
 * Kept minimal — all the data lives in the database.
 * Kafka is just the trigger, not the data transport.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDeliveryMessage {

    private UUID eventId;
}
