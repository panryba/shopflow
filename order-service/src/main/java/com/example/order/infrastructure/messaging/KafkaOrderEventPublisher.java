package com.example.order.infrastructure.messaging;

import com.example.order.application.port.output.OrderEventPublisher;
import com.example.shared.events.PaymentRequestEvent;
import com.example.shared.events.PaymentRollbackEvent;
import com.example.shared.events.RestaurantRequestEvent;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

@ApplicationScoped
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    @Channel("payment-request")
    Emitter<PaymentRequestEvent> paymentEmitter;

    @Channel("restaurant-request")
    Emitter<RestaurantRequestEvent> restaurantEmitter;

    @Channel("payment-rollback")
    Emitter<PaymentRollbackEvent> rollbackEmitter;

    @Override
    public void publishPaymentRequest(PaymentRequestEvent event) {
        sendWithKey(paymentEmitter, event.orderId().toString(), event);
    }

    @Override
    public void publishRestaurantRequest(RestaurantRequestEvent event) {
        sendWithKey(restaurantEmitter, event.orderId().toString(), event);
    }

    @Override
    public void publishPaymentRollback(PaymentRollbackEvent event) {
        sendWithKey(rollbackEmitter, event.orderId().toString(), event);
    }

    private <T> void sendWithKey(Emitter<T> emitter, String key, T payload) {
        var metadata = OutgoingKafkaRecordMetadata.builder()
                .withKey(key)
                .build();
        emitter.send(Message.of(payload).addMetadata(metadata));
    }
}
