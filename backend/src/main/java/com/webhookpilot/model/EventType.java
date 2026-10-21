package com.webhookpilot.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "event_types",
    uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "name"})
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    // Dotted convention: "order.created", "payment.failed"
    @Column(nullable = false)
    private String name;

    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Endpoints subscribed to this event type (inverse side of many-to-many)
    @ManyToMany(mappedBy = "subscribedEventTypes")
    @Builder.Default
    private Set<Endpoint> endpoints = new HashSet<>();
}
