package com.example.restaurant.infrastructure.messaging;

import com.example.shared.events.RestaurantApprovedEvent;
import com.example.shared.events.RestaurantRejectedEvent;
import com.example.shared.events.RestaurantRequestEvent;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.UUID;

@ApplicationScoped
public class RestaurantEventConsumer {

    @Channel("restaurant-approved")
    Emitter<RestaurantApprovedEvent> approvedEmitter;

    @Channel("restaurant-rejected")
    Emitter<RestaurantRejectedEvent> rejectedEmitter;

    @Incoming("restaurant-request")
    public void process(RestaurantRequestEvent event) {
        boolean accepted = false; // simulate failure

        if (accepted) {
            Log.info("RESTAURANT ACCEPTED");
            approvedEmitter.send(
                Message.of(RestaurantApprovedEvent.of(event.orderId()))
                    .addMetadata(key(event.orderId()))
            );
        } else {
            Log.info("RESTAURANT REJECTED");
            rejectedEmitter.send(
                Message.of(RestaurantRejectedEvent.of(event.orderId(), "No capacity"))
                    .addMetadata(key(event.orderId()))
            );
        }
    }

    private OutgoingKafkaRecordMetadata<UUID> key(UUID orderId) {
        return OutgoingKafkaRecordMetadata.<UUID>builder()
                .withKey(orderId)
                .build();
    }
}
