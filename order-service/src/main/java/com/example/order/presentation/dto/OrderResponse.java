package com.example.order.presentation.dto;

import com.example.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal total
) {}
