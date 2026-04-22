package com.example.shared.events;

import java.util.UUID;

public record RestaurantApprovedEvent(
        String eventId,
        UUID orderId
) {

    public static RestaurantApprovedEvent of(UUID orderId) {
        return new RestaurantApprovedEvent(
                UUID.randomUUID().toString(),
                orderId
        );
    }
}