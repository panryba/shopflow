package com.example.order.application.saga;

import com.example.order.application.port.input.OrderUseCase;
import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.infrastructure.outbox.OutboxEventType;
import com.example.order.infrastructure.outbox.OutboxService;
import com.example.order.infrastructure.persistence.idempotency.ProcessedEventRepository;
import com.example.order.presentation.dto.CreateOrderRequest;
import com.example.shared.events.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;

import java.util.UUID;

@ApplicationScoped
public class OrderSagaOrchestrator {

    @Inject
    OutboxService outbox;

    @Inject
    OrderUseCase service;

    @Inject
    ProcessedEventRepository processedRepository;

    @Transactional
    public UUID start(CreateOrderRequest request) {
        Order order = new Order(new OrderId(UUID.randomUUID()));
        request.items().forEach(i -> order.addItem(i.productId(), i.quantity(), i.price()));
        service.create(order);
        outbox.save(
                Order.class.getSimpleName(),
                order.getId().value().toString(),
                OutboxEventType.PAYMENT_REQUEST,
                PaymentRequestEvent.of(order.getId().value(), request.customerId(), request.amount())
        );
        return order.getId().value();
    }

    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        if (!tryProcess(event.eventId())) return;
        service.pay(new OrderId(event.orderId()));
        outbox.save(
                Order.class.getSimpleName(),
                event.orderId().toString(),
                OutboxEventType.RESTAURANT_REQUEST,
                RestaurantRequestEvent.of(event.orderId())
        );
    }

    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        if (!tryProcess(event.eventId())) return;
        service.cancel(new OrderId(event.orderId()));
    }

    @Transactional
    public void onRestaurantApproved(RestaurantApprovedEvent event) {
        if (!tryProcess(event.eventId())) return;
        service.approve(new OrderId(event.orderId()));
    }

    @Transactional
    public void onRestaurantRejected(RestaurantRejectedEvent event) {
        if (!tryProcess(event.eventId())) return;
        service.cancel(new OrderId(event.orderId()));
        outbox.save(
                Order.class.getSimpleName(),
                event.orderId().toString(),
                OutboxEventType.PAYMENT_ROLLBACK,
                PaymentRollbackEvent.of(event.orderId())
        );
    }

    private boolean tryProcess(String eventId) {
        try {
            processedRepository.save(eventId);
            return true;
        } catch (ConstraintViolationException e) {
            return false;
        }
    }
}