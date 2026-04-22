package com.example.payment.infrastructure.messaging;

import com.example.shared.events.PaymentCompletedEvent;
import com.example.shared.events.PaymentFailedEvent;
import com.example.shared.events.PaymentRequestEvent;
import com.example.shared.events.PaymentRollbackEvent;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.UUID;

@ApplicationScoped
public class PaymentEventConsumer {

    @Channel("payment-completed")
    Emitter<PaymentCompletedEvent> successEmitter;

    @Channel("payment-failed")
    Emitter<PaymentFailedEvent> failedEmitter;

    @Incoming("payment-request")
    public void process(PaymentRequestEvent event) {
        boolean success = processPayment(event);

        if (success) {
            Log.info("PAYMENT SUCCESS");
            successEmitter.send(
                Message.of(PaymentCompletedEvent.of(event.orderId(), event.correlationId()))
                    .addMetadata(key(event.orderId()))
            );
        } else {
            Log.info("PAYMENT FAILED");
            failedEmitter.send(
                Message.of(PaymentFailedEvent.of(event.orderId(), "Insufficient funds", event.correlationId()))
                    .addMetadata(key(event.orderId()))
            );
        }
    }

    @Incoming("payment-rollback")
    public void rollback(PaymentRollbackEvent event) {
        Log.infov("PAYMENT ROLLBACK id={0}", event.orderId());
    }

    private boolean processPayment(PaymentRequestEvent event) {
        return event.amount().doubleValue() < 1000;
    }

    private OutgoingKafkaRecordMetadata<String> key(UUID orderId) {
        return OutgoingKafkaRecordMetadata.<String>builder()
                .withKey(orderId.toString())
                .build();
    }
}
