package com.example.order.application.service;

import com.example.order.application.port.input.OrderUseCase;
import com.example.order.application.port.output.OrderRepository;
import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class OrderApplicationService implements OrderUseCase {

    @Inject
    OrderRepository repository;

    @Override
    public void create(Order order) {
        repository.save(order);
    }

    @Override
    public void pay(OrderId orderId) {
        Order order = repository.findById(orderId).orElseThrow();
        order.pay();
        repository.update(order);
    }

    @Override
    public void complete(OrderId orderId) {
        Order order = repository.findById(orderId).orElseThrow();
        order.complete();
        repository.update(order);
    }

    @Override
    public void cancel(OrderId orderId) {
        Order order = repository.findById(orderId).orElseThrow();
        order.cancel();
        repository.update(order);
    }

    @Override
    public Order findById(OrderId id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id.value()));
    }

    @Override
    public List<Order> findAllOrders() {
        return repository.findAllOrders();
    }
}