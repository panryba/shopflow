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
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column
    private String errorMessage;

    public enum Status {
        RECEIVED,
        PROCESSED,
        FAILED
    }
}
