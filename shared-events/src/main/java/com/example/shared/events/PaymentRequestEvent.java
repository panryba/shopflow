package com.example.shared.events;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestEvent(
        String eventId,
        UUID orderId,
        UUID customerId,
        BigDecimal amount
) {

    public static PaymentRequestEvent of(UUID orderId, UUID customerId, BigDecimal amount) {
        return new PaymentRequestEvent(
                UUID.randomUUID().toString(),
                orderId,
                customerId,
                amount
        );
    }
}