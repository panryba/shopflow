package com.example.order.infrastructure.messaging.deserializer;

import com.example.shared.events.RestaurantRejectedEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class RestaurantRejectedEventDeserializer extends ObjectMapperDeserializer<RestaurantRejectedEvent> {
    public RestaurantRejectedEventDeserializer() {
        super(RestaurantRejectedEvent.class);
    }
}