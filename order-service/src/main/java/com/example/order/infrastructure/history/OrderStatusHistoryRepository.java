package com.example.order.infrastructure.history;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class OrderStatusHistoryRepository implements PanacheRepositoryBase<OrderStatusHistory, UUID> {
}
