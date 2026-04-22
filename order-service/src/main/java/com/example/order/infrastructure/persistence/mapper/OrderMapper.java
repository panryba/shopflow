package com.example.order.infrastructure.persistence.mapper;

import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.infrastructure.persistence.entity.OrderEntity;
import com.example.order.infrastructure.persistence.entity.OrderItemEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class OrderMapper {

    public OrderEntity toEntity(Order order, OrderEntity entity) {
        entity.setId(order.getId().value());
        entity.setStatus(order.getStatus());

        Map<UUID, OrderItemEntity> existing = entity.getItems().stream()
                .collect(Collectors.toMap(OrderItemEntity::getId, item -> item));

        List<OrderItemEntity> items = order.getItems().stream()
                .map(item -> {
                    OrderItemEntity itemEntity = existing.getOrDefault(item.id(), new OrderItemEntity());
                    itemEntity.setId(item.id());
                    itemEntity.setProductId(item.productId());
                    itemEntity.setQuantity(item.quantity());
                    itemEntity.setPrice(item.price().amount());
                    itemEntity.setOrder(entity);
                    return itemEntity;
                })
                .toList();

        entity.getItems().clear();
        entity.getItems().addAll(items);
        return entity;
    }

    public Order toDomain(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(e -> new OrderItem(
                        e.getId(),
                        new OrderId(entity.getId()),
                        e.getProductId(),
                        e.getQuantity(),
                        new Money(e.getPrice())
                ))
                .toList();

        return Order.reconstitute(new OrderId(entity.getId()), items, entity.getStatus());
    }
}