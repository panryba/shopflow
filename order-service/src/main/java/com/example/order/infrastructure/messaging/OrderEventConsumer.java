package com.example.order.infrastructure.messaging;

import com.example.order.application.saga.OrderSagaOrchestrator;
import com.example.order.infrastructure.observability.CorrelationIdProvider;
import com.example.shared.events.*;
import io.quarkus.logging.Log;
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

@ApplicationScoped
public class OrderEventConsumer {

    @Inject
    OrderSagaOrchestrator orchestrator;

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Incoming("payment-completed")
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public void onPaymentCompleted(Message<PaymentCompletedEvent> message) {
        try {
            PaymentCompletedEvent event = message.getPayload();
            setMDC(message, event.orderId());
            orchestrator.onPaymentCompleted(event);
        } finally {
            clearMDC();
        }
    }

    @Incoming("payment-failed")
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public void onPaymentFailed(Message<PaymentFailedEvent> message) {
        try {
            PaymentFailedEvent event = message.getPayload();
            setMDC(message, event.orderId());
            orchestrator.onPaymentFailed(event);
        } finally {
            clearMDC();
        }
    }

    @Incoming("restaurant-approved")
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public void onRestaurantApproved(Message<RestaurantApprovedEvent> message) {
        try {
            RestaurantApprovedEvent event = message.getPayload();
            setMDC(message, event.orderId());
            orchestrator.onRestaurantApproved(event);
        } finally {
            clearMDC();
        }
    }

    @Incoming("restaurant-rejected")
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public void onRestaurantRejected(Message<RestaurantRejectedEvent> message) {
        try {
            RestaurantRejectedEvent event = message.getPayload();
            setMDC(message, event.orderId());
            orchestrator.onRestaurantRejected(event);
        } finally {
            clearMDC();
        }
    }

    private void setMDC(Message<?> message, UUID orderId) {
        String correlationId = extractCorrelationId(message);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            Log.warnf("Missing X-Correlation-ID header — generated fallback corrId=%s", correlationId);
        }
        correlationIdProvider.set(correlationId);
        MDC.put("orderId", orderId.toString());
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
