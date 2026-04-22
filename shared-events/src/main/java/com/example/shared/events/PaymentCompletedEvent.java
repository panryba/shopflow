package com.example.shared.events;

import java.util.UUID;

public record PaymentCompletedEvent(
        String eventId,
        UUID orderId
) {

    public static PaymentCompletedEvent of(UUID orderId) {
        return new PaymentCompletedEvent(
                UUID.randomUUID().toString(),
                orderId
        );
    }
}