package com.example.payment.infrastructure.messaging;

import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class PaymentEventConsumer {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Channel("payment-completed")
    Emitter<com.example.order.events.avro.PaymentCompletedEvent> successEmitter;

    @Channel("payment-failed")
    Emitter<com.example.order.events.avro.PaymentFailedEvent> failedEmitter;

    @Incoming("payment-request")
    public CompletionStage<Void> process(Message<com.example.order.events.avro.PaymentRequestEvent> message) {
        try {
            var avro = message.getPayload();
            boolean success = processPayment(avro.getAmount().toString());
            String orderId = avro.getOrderId().toString();
            String correlationId = avro.getCorrelationId().toString();

            if (success) {
                Log.info("PAYMENT SUCCESS");
                successEmitter.send(Message.of(
                        com.example.order.events.avro.PaymentCompletedEvent.newBuilder()
                                .setEventId(UUID.randomUUID().toString())
                                .setOrderId(orderId)
                                .setCorrelationId(correlationId)
                                .build())
                        .addMetadata(key(orderId, correlationId)));
            } else {
                Log.info("PAYMENT FAILED");
                failedEmitter.send(Message.of(
                        com.example.order.events.avro.PaymentFailedEvent.newBuilder()
                                .setEventId(UUID.randomUUID().toString())
                                .setOrderId(orderId)
                                .setReason("Insufficient funds")
                                .setCorrelationId(correlationId)
                                .build())
                        .addMetadata(key(orderId, correlationId)));
            }
            return message.ack();
        } catch (Exception e) {
            return message.nack(e);
        }
    }

    @Incoming("payment-rollback")
    public CompletionStage<Void> rollback(Message<com.example.order.events.avro.PaymentRollbackEvent> message) {
        try {
            Log.infov("PAYMENT ROLLBACK id={0}", message.getPayload().getOrderId().toString());
            return message.ack();
        } catch (Exception e) {
            return message.nack(e);
        }
    }

    private boolean processPayment(String amountStr) {
        return new BigDecimal(amountStr).doubleValue() < 1000;
    }

    private OutgoingKafkaRecordMetadata<String> key(String orderId, String correlationId) {
        return OutgoingKafkaRecordMetadata.<String>builder()
                .withKey(orderId)
                .withHeaders(new RecordHeaders()
                        .add(CORRELATION_ID_HEADER, correlationId.getBytes(StandardCharsets.UTF_8)))
                .build();
    }
}
