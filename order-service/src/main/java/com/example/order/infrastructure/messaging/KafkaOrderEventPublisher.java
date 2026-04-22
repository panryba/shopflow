package com.example.order.infrastructure.messaging;

import com.example.order.application.port.output.OrderEventPublisher;
import com.example.shared.events.PaymentRequestEvent;
import com.example.shared.events.PaymentRollbackEvent;
import com.example.shared.events.RestaurantRequestEvent;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@ApplicationScoped
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Channel("payment-request")
    Emitter<PaymentRequestEvent> paymentEmitter;

    @Channel("restaurant-request")
    Emitter<RestaurantRequestEvent> restaurantEmitter;

    @Channel("payment-rollback")
    Emitter<PaymentRollbackEvent> rollbackEmitter;

    @Override
    public void publishPaymentRequest(PaymentRequestEvent event) {
        sendWithCorrelation(paymentEmitter, event.orderId(), event.correlationId(), event);
    }

    @Override
    public void publishRestaurantRequest(RestaurantRequestEvent event) {
        sendWithCorrelation(restaurantEmitter, event.orderId(), event.correlationId(), event);
    }

    @Override
    public void publishPaymentRollback(PaymentRollbackEvent event) {
        sendWithCorrelation(rollbackEmitter, event.orderId(), event.correlationId(), event);
    }

    private <T> void sendWithCorrelation(Emitter<T> emitter, UUID key, String correlationId, T payload) {
        var metadata = OutgoingKafkaRecordMetadata.<String>builder()
                .withKey(key.toString())
                .withHeaders(new RecordHeaders()
                        .add(CORRELATION_ID_HEADER, correlationId.getBytes(StandardCharsets.UTF_8)))
                .build();
        emitter.send(Message.of(payload).addMetadata(metadata));
    }
}
