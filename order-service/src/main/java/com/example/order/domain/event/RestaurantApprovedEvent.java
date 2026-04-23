package com.example.order.domain.event;

import java.util.UUID;

public record RestaurantApprovedEvent(
        String eventId,
        UUID orderId,
        String correlationId
) {
    public static RestaurantApprovedEvent of(UUID orderId, String correlationId) {
        return new RestaurantApprovedEvent(UUID.randomUUID().toString(), orderId, correlationId);
    }
}