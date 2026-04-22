package com.example.restaurant.infrastructure.messaging.deserializer;

import com.example.shared.events.RestaurantRequestEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class RestaurantRequestEventDeserializer extends ObjectMapperDeserializer<RestaurantRequestEvent> {
    public RestaurantRequestEventDeserializer() {
        super(RestaurantRequestEvent.class);
    }
}