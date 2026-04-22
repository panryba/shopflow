package com.example.shared.events;

import java.util.UUID;

public record RestaurantRequestEvent(
        String eventId,
        UUID orderId
) {

    public static RestaurantRequestEvent of(UUID orderId) {
        return new RestaurantRequestEvent(
                UUID.randomUUID().toString(),
                orderId
        );
    }
}