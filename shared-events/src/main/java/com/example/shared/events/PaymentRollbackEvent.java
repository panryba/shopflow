package com.example.shared.events;

import java.util.UUID;

public record PaymentRollbackEvent(
        String eventId,
        UUID orderId
) {

    public static PaymentRollbackEvent of(UUID orderId) {
        return new PaymentRollbackEvent(
                UUID.randomUUID().toString(),
                orderId
        );
    }
}