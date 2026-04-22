package com.example.order.infrastructure.messaging.deserializer;

import com.example.shared.events.PaymentFailedEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class PaymentFailedEventDeserializer extends ObjectMapperDeserializer<PaymentFailedEvent> {
    public PaymentFailedEventDeserializer() {
        super(PaymentFailedEvent.class);
    }
}