package com.example.order.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull
        UUID customerId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        @NotEmpty
        @Valid
        List<OrderItemRequest> items
) {}
