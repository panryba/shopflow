package com.example.inventory.infrastructure.messaging;

import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class InventoryEventConsumer {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Channel("inventory-approved")
    Emitter<com.example.order.events.avro.InventoryApprovedEvent> approvedEmitter;

    @Channel("inventory-rejected")
    Emitter<com.example.order.events.avro.InventoryRejectedEvent> rejectedEmitter;

    volatile boolean accepted = true;

    @Incoming("inventory-request")
    public CompletionStage<Void> process(Message<com.example.order.events.avro.InventoryRequestEvent> message) {
        try {

            var avro = message.getPayload();
            String orderId = avro.getOrderId().toString();
            String correlationId = avro.getCorrelationId().toString();

            if (accepted) {
                Log.info("INVENTORY ACCEPTED");
                approvedEmitter.send(Message.of(
                        com.example.order.events.avro.InventoryApprovedEvent.newBuilder()
                                .setEventId(UUID.randomUUID().toString())
                                .setOrderId(orderId)
                                .setCorrelationId(correlationId)
                                .build())
                        .addMetadata(key(orderId, correlationId)));
            } else {
                Log.info("INVENTORY REJECTED");
                rejectedEmitter.send(Message.of(
                        com.example.order.events.avro.InventoryRejectedEvent.newBuilder()
                                .setEventId(UUID.randomUUID().toString())
                                .setOrderId(orderId)
                                .setReason("Out of stock")
                                .setCorrelationId(correlationId)
                                .build())
                        .addMetadata(key(orderId, correlationId)));
            }
            return message.ack();
        } catch (Exception e) {
            return message.nack(e);
        }
    }

    private OutgoingKafkaRecordMetadata<String> key(String orderId, String correlationId) {
        return OutgoingKafkaRecordMetadata.<String>builder()
                .withKey(orderId)
                .withHeaders(new RecordHeaders()
                        .add(CORRELATION_ID_HEADER, correlationId.getBytes(StandardCharsets.UTF_8)))
                .build();
    }
}
