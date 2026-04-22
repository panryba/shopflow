package com.example.order.application.port.output;

import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
    void update(Order order);
    List<Order> findAll();
}