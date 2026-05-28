package com.example.order.infrastructure.sse;

import com.example.order.domain.event.OrderSagaCompletedEvent;
import com.example.order.domain.event.OrderStatusChangedEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class OrderSseService {

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<MultiEmitter<? super String>>> emitters =
            new ConcurrentHashMap<>();

    void onStatusChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderStatusChangedEvent event) {
        var list = emitters.get(event.orderId());
        if (list == null) return;
        String status = event.status().name();
        list.forEach(e -> e.emit(status));
    }

    void onSagaCompleted(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderSagaCompletedEvent event) {
        var list = emitters.remove(event.orderId());
        if (list != null) list.forEach(MultiEmitter::complete);
    }

    public Multi<String> stream(UUID orderId) {
        return Multi.createFrom().emitter(emitter -> {
            emitters.computeIfAbsent(orderId, _ -> new CopyOnWriteArrayList<>()).add(emitter);
            emitter.onTermination(() -> {
                var list = emitters.get(orderId);
                if (list != null) {
                    list.remove(emitter);
                    if (list.isEmpty()) emitters.remove(orderId);
                }
            });
        });
    }
}