package com.example.order.infrastructure.messaging.deserializer;

import com.example.shared.events.PaymentCompletedEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class PaymentCompletedEventDeserializer extends ObjectMapperDeserializer<PaymentCompletedEvent> {
    public PaymentCompletedEventDeserializer() {
        super(PaymentCompletedEvent.class);
    }
}