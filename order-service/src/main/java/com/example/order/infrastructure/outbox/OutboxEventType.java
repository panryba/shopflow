package com.example.order.infrastructure.outbox;

public enum OutboxEventType {
    PAYMENT_REQUEST,
    RESTAURANT_REQUEST,
    PAYMENT_ROLLBACK
}