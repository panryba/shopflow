package com.example.payment.infrastructure.messaging.deserializer;

import com.example.shared.events.PaymentRequestEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class PaymentRequestEventDeserializer extends ObjectMapperDeserializer<PaymentRequestEvent> {
    public PaymentRequestEventDeserializer() {
        super(PaymentRequestEvent.class);
    }
}
