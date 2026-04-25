package com.example.order.domain.model;

import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.OrderId;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Order {

    private OrderId id;
    private List<OrderItem> items = new ArrayList<>();
    private OrderStatus status;

    public Order(OrderId id) {
        this.id = id;
        this.status = OrderStatus.PENDING;
    }

    public static Order reconstitute(OrderId id, List<OrderItem> items, OrderStatus status) {
        Order order = new Order(id);
        order.items = new ArrayList<>(items);
        order.status = status;
        return order;
    }

    public void addItem(UUID productId, int quantity, BigDecimal price) {
        if (quantity <= 0) throw new IllegalArgumentException();
        items.add(new OrderItem(UUID.randomUUID(), this.id, productId, quantity, new Money(price)));
    }

    public Money totalAmount() {
        if (items.isEmpty()) throw new IllegalStateException("Order has no items");
        return items.stream()
                .map(i -> i.price().multiply(i.quantity()))
                .reduce(Money.ZERO, Money::add);
    }

    public void pay() {
        if (status == OrderStatus.CANCELLED) return;
        if (status != OrderStatus.PENDING) throw new IllegalStateException("Invalid state transition");
        this.status = OrderStatus.PAID;
    }

    public void complete() {
        if (status == OrderStatus.CANCELLED) return;
        if (status != OrderStatus.PAID) throw new IllegalStateException("Cannot complete");
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) return;
        this.status = OrderStatus.CANCELLED;
    }
}