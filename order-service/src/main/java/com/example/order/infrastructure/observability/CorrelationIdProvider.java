package com.example.order.infrastructure.observability;

import java.util.UUID;

public class CorrelationIdProvider {

    public static String get() {
        return UUID.randomUUID().toString();
    }
}