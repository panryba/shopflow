package com.example.payment.infrastructure.messaging.deserializer;

import com.example.shared.events.PaymentRollbackEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class PaymentRollbackEventDeserializer extends ObjectMapperDeserializer<PaymentRollbackEvent> {
    public PaymentRollbackEventDeserializer() {
        super(PaymentRollbackEvent.class);
    }
}