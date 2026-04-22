package com.example.order.infrastructure.messaging.deserializer;

import com.example.shared.events.RestaurantApprovedEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class RestaurantApprovedEventDeserializer extends ObjectMapperDeserializer<RestaurantApprovedEvent> {
    public RestaurantApprovedEventDeserializer() {
        super(RestaurantApprovedEvent.class);
    }
}