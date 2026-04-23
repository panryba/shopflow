package com.example.order.infrastructure.messaging;

import com.example.order.application.saga.OrderSagaOrchestrator;
import com.example.order.domain.event.*;
import com.example.order.infrastructure.observability.CorrelationIdProvider;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.MDC;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class OrderEventConsumer {

    @Inject
    OrderSagaOrchestrator orchestrator;

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Incoming("payment-completed")
    @Blocking
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public CompletionStage<Void> onPaymentCompleted(Message<com.example.order.events.avro.PaymentCompletedEvent> message) {
        try {
            var avro = message.getPayload();
            setMDC(message, avro.getOrderId().toString());
            orchestrator.onPaymentCompleted(new PaymentCompletedEvent(
                    avro.getEventId().toString(), UUID.fromString(avro.getOrderId().toString()), avro.getCorrelationId().toString()));
            return message.ack();
        } catch (Exception e) {
            Log.errorf(e, "onPaymentCompleted failed: %s", e.getMessage());
            return message.nack(e);
        } finally {
            clearMDC();
        }
    }

    @Incoming("payment-failed")
    @Blocking
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public CompletionStage<Void> onPaymentFailed(Message<com.example.order.events.avro.PaymentFailedEvent> message) {
        try {
            var avro = message.getPayload();
            setMDC(message, avro.getOrderId().toString());
            orchestrator.onPaymentFailed(new PaymentFailedEvent(
                    avro.getEventId().toString(),
                    UUID.fromString(avro.getOrderId().toString()),
                    avro.getReason() != null ? avro.getReason().toString() : null,
                    avro.getCorrelationId().toString()));
            return message.ack();
        } catch (Exception e) {
            return message.nack(e);
        } finally {
            clearMDC();
        }
    }

    @Incoming("restaurant-approved")
    @Blocking
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public CompletionStage<Void> onRestaurantApproved(Message<com.example.order.events.avro.RestaurantApprovedEvent> message) {
        try {
            var avro = message.getPayload();
            setMDC(message, avro.getOrderId().toString());
            orchestrator.onRestaurantApproved(new RestaurantApprovedEvent(
                    avro.getEventId().toString(), UUID.fromString(avro.getOrderId().toString()), avro.getCorrelationId().toString()));
            return message.ack();
        } catch (Exception e) {
            return message.nack(e);
        } finally {
            clearMDC();
        }
    }

    @Incoming("restaurant-rejected")
    @Blocking
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public CompletionStage<Void> onRestaurantRejected(Message<com.example.order.events.avro.RestaurantRejectedEvent> message) {
        try {
            var avro = message.getPayload();
            setMDC(message, avro.getOrderId().toString());
            orchestrator.onRestaurantRejected(new RestaurantRejectedEvent(
                    avro.getEventId().toString(),
                    UUID.fromString(avro.getOrderId().toString()),
                    avro.getReason() != null ? avro.getReason().toString() : null,
                    avro.getCorrelationId().toString()));
            return message.ack();
        } catch (Exception e) {
            return message.nack(e);
        } finally {
            clearMDC();
        }
    }

    private void setMDC(Message<?> message, String orderId) {
        String correlationId = extractCorrelationId(message);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            Log.warnf("Missing X-Correlation-ID header — generated fallback corrId=%s", correlationId);
        }
        correlationIdProvider.set(correlationId);
        MDC.put("orderId", orderId);
    }

    private void clearMDC() {
        MDC.clear();
    }

    private String extractCorrelationId(Message<?> message) {
        return message.getMetadata(IncomingKafkaRecordMetadata.class)
                .map(meta -> meta.getHeaders().lastHeader("X-Correlation-ID"))
                .map(header -> new String(header.value(), StandardCharsets.UTF_8))
                .orElse(null);
    }
}
