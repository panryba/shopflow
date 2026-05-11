package com.example.order.domain.event;

import com.example.order.domain.model.HistoryStatus;

import java.util.UUID;

public record OrderStatusChangedEvent(UUID orderId, HistoryStatus status) {}