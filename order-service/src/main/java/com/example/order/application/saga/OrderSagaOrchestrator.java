package com.example.order.application.saga;

import com.example.order.application.port.input.OrderUseCase;
import com.example.order.domain.event.*;
import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.infrastructure.inbox.InboxService;
import com.example.order.infrastructure.observability.CorrelationIdProvider;
import com.example.order.infrastructure.outbox.OutboxEventType;
import com.example.order.infrastructure.outbox.OutboxService;
import com.example.order.presentation.dto.CreateOrderRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class OrderSagaOrchestrator {

    @Inject OutboxService outbox;
    @Inject OrderUseCase service;
    @Inject CorrelationIdProvider correlationIdProvider;
    @Inject OrderSagaRepository sagaRepository;
    @Inject InboxService inbox;

    @Transactional
    public UUID start(CreateOrderRequest request) {
        String correlationId = getCorrelationId();

        Order order = new Order(new OrderId(UUID.randomUUID()));
        request.items().forEach(i -> order.addItem(i.productId(), i.quantity(), i.price()));
        service.create(order);

        sagaRepository.save(
                OrderSagaState.builder()
                        .orderId(order.getId().value())
                        .step(OrderSagaState.SagaStep.WAITING_PAYMENT)
                        .deadline(Instant.now().plusSeconds(30))
                        .correlationId(correlationId)
                        .build()
        );

        outbox.save(
                Order.class.getSimpleName(),
                order.getId().value().toString(),
                OutboxEventType.PAYMENT_REQUEST,
                PaymentRequestEvent.of(order.getId().value(), request.customerId(), request.amount(), correlationId)
        );

        return order.getId().value();
    }

    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        if (!inbox.receive(event.eventId(), "PaymentCompleted")) return;

        try {
            OrderSagaState saga = sagaRepository.find(event.orderId());
            if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

            service.pay(new OrderId(event.orderId()));

            saga.setStep(OrderSagaState.SagaStep.WAITING_RESTAURANT);
            saga.setDeadline(Instant.now().plusSeconds(30));

            outbox.save(
                    Order.class.getSimpleName(),
                    event.orderId().toString(),
                    OutboxEventType.RESTAURANT_REQUEST,
                    RestaurantRequestEvent.of(event.orderId(), event.correlationId())
            );

            inbox.markProcessed(event.eventId());

        } catch (Exception e) {
            inbox.markFailed(event.eventId(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        if (!inbox.receive(event.eventId(), "PaymentFailed")) return;

        try {
            OrderSagaState saga = sagaRepository.find(event.orderId());
            if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

            service.cancel(new OrderId(event.orderId()));
            saga.setStep(OrderSagaState.SagaStep.CANCELLED);

            inbox.markProcessed(event.eventId());

        } catch (Exception e) {
            inbox.markFailed(event.eventId(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void onRestaurantApproved(RestaurantApprovedEvent event) {
        if (!inbox.receive(event.eventId(), "RestaurantApproved")) return;

        try {
            OrderSagaState saga = sagaRepository.find(event.orderId());
            if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

            service.approve(new OrderId(event.orderId()));
            saga.setStep(OrderSagaState.SagaStep.COMPLETED);

            inbox.markProcessed(event.eventId());

        } catch (Exception e) {
            inbox.markFailed(event.eventId(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void onRestaurantRejected(RestaurantRejectedEvent event) {
        if (!inbox.receive(event.eventId(), "RestaurantRejected")) return;

        try {
            OrderSagaState saga = sagaRepository.find(event.orderId());
            if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

            service.cancel(new OrderId(event.orderId()));

            outbox.save(
                    Order.class.getSimpleName(),
                    event.orderId().toString(),
                    OutboxEventType.PAYMENT_ROLLBACK,
                    PaymentRollbackEvent.of(event.orderId(), event.correlationId())
            );

            saga.setStep(OrderSagaState.SagaStep.CANCELLED);

            inbox.markProcessed(event.eventId());

        } catch (Exception e) {
            inbox.markFailed(event.eventId(), e.getMessage());
            throw e;
        }
    }

    private String getCorrelationId() {
        String id = correlationIdProvider.get();
        return id != null ? id : UUID.randomUUID().toString();
    }
}
