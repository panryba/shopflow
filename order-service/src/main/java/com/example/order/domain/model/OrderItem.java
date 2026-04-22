package com.example.order.domain.model;

import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.OrderId;

import java.util.UUID;

public record OrderItem(
        UUID id,
        OrderId orderId,
        UUID productId,
        int quantity,
        Money price
) {}