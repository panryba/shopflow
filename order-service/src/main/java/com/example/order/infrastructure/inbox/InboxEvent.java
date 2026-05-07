package com.example.order.infrastructure.inbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "inbox_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboxEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "error_message")
    private String errorMessage;

    public enum Status {
        RECEIVED,
        PROCESSED,
        FAILED
    }
}
