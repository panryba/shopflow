package com.example.order.domain.event;

import java.util.UUID;

public record RestaurantRequestEvent(
        String eventId,
        UUID orderId,
        String correlationId
) {
    public static RestaurantRequestEvent of(UUID orderId, String correlationId) {
        return new RestaurantRequestEvent(UUID.randomUUID().toString(), orderId, correlationId);
    }
}