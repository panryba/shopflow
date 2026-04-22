package com.example.order.infrastructure.messaging;

import com.example.order.application.saga.OrderSagaOrchestrator;
import com.example.order.infrastructure.persistence.idempotency.ProcessedEventRepository;
import com.example.shared.events.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class OrderEventConsumer {

    @Inject
    OrderSagaOrchestrator orchestrator;

    @Inject
    ProcessedEventRepository processedRepository;

    @Incoming("payment-completed")
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        orchestrator.onPaymentCompleted(event);
    }

    @Incoming("payment-failed")
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public void onPaymentFailed(PaymentFailedEvent event) {
        orchestrator.onPaymentFailed(event);
    }

    @Incoming("restaurant-approved")
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public void onRestaurantApproved(RestaurantApprovedEvent event) {
        orchestrator.onRestaurantApproved(event);
    }

    @Incoming("restaurant-rejected")
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public void onRestaurantRejected(RestaurantRejectedEvent event) {
        orchestrator.onRestaurantRejected(event);
    }
}