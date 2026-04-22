package com.example.shared.events;

import java.util.UUID;

public record RestaurantRejectedEvent(
        String eventId,
        UUID orderId,
        String reason
) {

    public static RestaurantRejectedEvent of(UUID orderId, String reason) {
        return new RestaurantRejectedEvent(
                UUID.randomUUID().toString(),
                orderId,
                reason
        );
    }
}