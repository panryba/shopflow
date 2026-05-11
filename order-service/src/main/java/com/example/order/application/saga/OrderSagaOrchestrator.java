package com.example.order.application.saga;

import com.example.order.application.port.input.OrderUseCase;
import com.example.order.application.port.output.OrderRepository;
import com.example.order.domain.event.InventoryApprovedEvent;
import com.example.order.domain.event.InventoryRejectedEvent;
import com.example.order.domain.event.InventoryRequestEvent;
import com.example.order.domain.event.PaymentCompletedEvent;
import com.example.order.domain.event.PaymentFailedEvent;
import com.example.order.domain.event.PaymentRequestEvent;
import com.example.order.domain.event.PaymentRollbackCompletedEvent;
import com.example.order.domain.event.PaymentRollbackEvent;
import com.example.order.domain.model.HistoryStatus;
import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.infrastructure.history.OrderStatusHistoryService;
import com.example.order.infrastructure.inbox.InboxService;
import com.example.order.infrastructure.observability.CorrelationIdProvider;
import com.example.order.infrastructure.outbox.OutboxEventType;
import com.example.order.infrastructure.outbox.OutboxService;
import com.example.order.presentation.dto.CreateOrderRequest;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.persistence.PersistenceException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrderSagaOrchestrator {

    @Inject OutboxService outbox;
    @Inject OrderUseCase service;
    @Inject OrderRepository orderRepository;
    @Inject CorrelationIdProvider correlationIdProvider;
    @Inject OrderSagaRepository sagaRepository;
    @Inject InboxService inbox;
    @Inject OrderStatusHistoryService historyService;

    @Transactional
    public UUID start(CreateOrderRequest request, UUID customerId, String username, UUID idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Log.infof("Duplicate request detected idempotencyKey=%s orderId=%s", idempotencyKey, existing.get().getId().value());
                return existing.get().getId().value();
            }
        }

        try {
            String correlationId = getCorrelationId();

            Order order = new Order(new OrderId(UUID.randomUUID()));
            order.setIdempotencyKey(idempotencyKey);
            order.setUserId(customerId);
            order.setUsername(username);
            request.items().forEach(i -> order.addItem(i.productId(), i.quantity(), i.price()));
            var total = order.totalAmount();

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
                    PaymentRequestEvent.of(order.getId().value(), customerId, total.amount(), correlationId)
            );

            Log.infof("Saga started orderId=%s step=WAITING_PAYMENT total=%s", order.getId().value(), total.amount());
            return order.getId().value();

        } catch (PersistenceException e) {
            if (isUniqueConstraintViolation(e) && idempotencyKey != null) {
                Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    Log.infof("Race condition resolved idempotencyKey=%s orderId=%s", idempotencyKey, existing.get().getId().value());
                    return existing.get().getId().value();
                }
            }
            throw e;
        }
    }

    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        if (!inbox.receive(event.eventId(), "PaymentCompleted")) return;

        try {
            OrderSagaState saga = sagaRepository.find(event.orderId());
            if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

            service.pay(new OrderId(event.orderId()));

            saga.setStep(OrderSagaState.SagaStep.WAITING_INVENTORY);
            saga.setDeadline(Instant.now().plusSeconds(30));

            outbox.save(
                    Order.class.getSimpleName(),
                    event.orderId().toString(),
                    OutboxEventType.INVENTORY_REQUEST,
                    InventoryRequestEvent.of(event.orderId(), event.correlationId())
            );

            Log.infof("Payment completed orderId=%s step=WAITING_INVENTORY", event.orderId());
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

            service.failPayment(new OrderId(event.orderId()));
            saga.setStep(OrderSagaState.SagaStep.CANCELLED);

            Log.infof("Payment failed orderId=%s step=CANCELLED reason=%s", event.orderId(), event.reason());
            inbox.markProcessed(event.eventId());

        } catch (Exception e) {
            inbox.markFailed(event.eventId(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void onInventoryApproved(InventoryApprovedEvent event) {
        if (!inbox.receive(event.eventId(), "InventoryApproved")) return;

        try {
            OrderSagaState saga = sagaRepository.find(event.orderId());
            if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

            service.approveInventory(new OrderId(event.orderId()));
            saga.setStep(OrderSagaState.SagaStep.COMPLETED);

            Log.infof("Inventory approved orderId=%s step=COMPLETED", event.orderId());
            inbox.markProcessed(event.eventId());

        } catch (Exception e) {
            inbox.markFailed(event.eventId(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void onInventoryRejected(InventoryRejectedEvent event) {
        if (!inbox.receive(event.eventId(), "InventoryRejected")) return;

        try {
            OrderSagaState saga = sagaRepository.find(event.orderId());
            if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

            service.rejectInventory(new OrderId(event.orderId()));

            outbox.save(
                    Order.class.getSimpleName(),
                    event.orderId().toString(),
                    OutboxEventType.PAYMENT_ROLLBACK,
                    PaymentRollbackEvent.of(event.orderId(), event.correlationId())
            );

            saga.setStep(OrderSagaState.SagaStep.WAITING_ROLLBACK);
            saga.setDeadline(Instant.now().plusSeconds(30));

            Log.infof("Inventory rejected orderId=%s step=WAITING_ROLLBACK reason=%s", event.orderId(), event.reason());
            inbox.markProcessed(event.eventId());

        } catch (Exception e) {
            inbox.markFailed(event.eventId(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void onPaymentRolledBack(PaymentRollbackCompletedEvent event) {
        if (!inbox.receive(event.eventId(), "PaymentRollbackCompleted")) return;

        try {
            OrderSagaState saga = sagaRepository.find(event.orderId());
            if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

            historyService.record(event.orderId(), HistoryStatus.PAYMENT_ROLLED_BACK);
            saga.setStep(OrderSagaState.SagaStep.CANCELLED);

            Log.infof("Payment rolled back orderId=%s step=CANCELLED", event.orderId());
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

    private boolean isUniqueConstraintViolation(Throwable t) {
        while (t != null) {
            if (t instanceof org.hibernate.exception.ConstraintViolationException) return true;
            t = t.getCause();
        }
        return false;
    }
}
