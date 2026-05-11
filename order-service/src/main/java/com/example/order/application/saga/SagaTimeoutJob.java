package com.example.order.application.saga;

import com.example.order.application.port.input.OrderUseCase;
import com.example.order.domain.event.PaymentRollbackEvent;
import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.infrastructure.outbox.OutboxEventType;
import com.example.order.infrastructure.outbox.OutboxService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class SagaTimeoutJob {

    @Inject OrderSagaRepository sagaRepository;
    @Inject OrderUseCase orderService;
    @Inject OutboxService outbox;

    @Scheduled(every = "10s")
    @Transactional
    void checkTimeouts() {
        var expired = sagaRepository.findExpired(Instant.now());
        for (OrderSagaState saga : expired) {
            handleTimeout(saga);
        }
    }

    private void handleTimeout(OrderSagaState saga) {
        var orderId = new OrderId(saga.getOrderId());

        switch (saga.getStep()) {
            case WAITING_PAYMENT -> orderService.cancel(orderId);
            case WAITING_INVENTORY -> {
                orderService.cancel(orderId);
                outbox.save(
                        Order.class.getSimpleName(),
                        saga.getOrderId().toString(),
                        OutboxEventType.PAYMENT_ROLLBACK,
                        PaymentRollbackEvent.of(saga.getOrderId(), saga.getCorrelationId())
                );
            }
            case WAITING_ROLLBACK -> orderService.cancel(orderId);
            default -> { return; }
        }

        saga.setStep(OrderSagaState.SagaStep.CANCELLED);
    }
}
