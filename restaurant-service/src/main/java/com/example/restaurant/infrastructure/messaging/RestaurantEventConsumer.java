package com.example.restaurant.infrastructure.messaging;

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
public class RestaurantEventConsumer {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Channel("restaurant-approved")
    Emitter<com.example.order.events.avro.RestaurantApprovedEvent> approvedEmitter;

    @Channel("restaurant-rejected")
    Emitter<com.example.order.events.avro.RestaurantRejectedEvent> rejectedEmitter;

    @Incoming("restaurant-request")
    public CompletionStage<Void> process(Message<com.example.order.events.avro.RestaurantRequestEvent> message) {
        try {
            boolean accepted = false; // simulate failure

            var avro = message.getPayload();
            String orderId = avro.getOrderId().toString();
            String correlationId = avro.getCorrelationId().toString();

            if (accepted) {
                Log.info("RESTAURANT ACCEPTED");
                approvedEmitter.send(Message.of(
                        com.example.order.events.avro.RestaurantApprovedEvent.newBuilder()
                                .setEventId(UUID.randomUUID().toString())
                                .setOrderId(orderId)
                                .setCorrelationId(correlationId)
                                .build())
                        .addMetadata(key(orderId, correlationId)));
            } else {
                Log.info("RESTAURANT REJECTED");
                rejectedEmitter.send(Message.of(
                        com.example.order.events.avro.RestaurantRejectedEvent.newBuilder()
                                .setEventId(UUID.randomUUID().toString())
                                .setOrderId(orderId)
                                .setReason("No capacity")
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
