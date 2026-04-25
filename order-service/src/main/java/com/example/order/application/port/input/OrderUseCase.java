package com.example.order.application.port.input;

import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;

import java.util.List;

public interface OrderUseCase {
    void create(Order order);
    void pay(OrderId orderId);
    void complete(OrderId orderId);
    void cancel(OrderId orderId);
    Order findById(OrderId id);
    List<Order> findAllOrders();
}