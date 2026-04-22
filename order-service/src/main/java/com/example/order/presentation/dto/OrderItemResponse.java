package com.example.order.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        int quantity,
        BigDecimal price
) {}