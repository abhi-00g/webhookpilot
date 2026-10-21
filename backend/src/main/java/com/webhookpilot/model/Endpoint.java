package com.webhookpilot.model;

import com.webhookpilot.model.enums.EndpointStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "endpoints")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    // The subscriber's URL we POST events to
    @Column(nullable = false)
    private String url;

    // Auto-generated secret for HMAC-SHA256 signature verification
    @Column(nullable = false)
    private String secret;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EndpointStatus status = EndpointStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Owning side of many-to-many: which event types this endpoint subscribes to
    @ManyToMany
    @JoinTable(
        name = "endpoint_event_types",
        joinColumns = @JoinColumn(name = "endpoint_id"),
        inverseJoinColumns = @JoinColumn(name = "event_type_id")
    )
    @Builder.Default
    private Set<EventType> subscribedEventTypes = new HashSet<>();
}
