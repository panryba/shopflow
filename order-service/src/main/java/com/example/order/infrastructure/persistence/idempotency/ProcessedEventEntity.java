package com.example.order.infrastructure.persistence.idempotency;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "order_processed_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEventEntity {

    @Id
    private String eventId;

    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    @PrePersist
    void prePersist() {
        this.processedAt = Instant.now();
    }
}