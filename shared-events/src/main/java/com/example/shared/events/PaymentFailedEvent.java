package com.example.shared.events;

import java.util.UUID;

public record PaymentFailedEvent(
        String eventId,
        UUID orderId,
        String reason
) {

    public static PaymentFailedEvent of(UUID orderId, String reason) {
        return new PaymentFailedEvent(
                UUID.randomUUID().toString(),
                orderId,
                reason
        );
    }
}