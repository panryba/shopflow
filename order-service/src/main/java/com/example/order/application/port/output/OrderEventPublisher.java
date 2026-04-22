package com.example.order.application.port.output;

import com.example.shared.events.PaymentRequestEvent;
import com.example.shared.events.PaymentRollbackEvent;
import com.example.shared.events.RestaurantRequestEvent;

public interface OrderEventPublisher {
    void publishPaymentRequest(PaymentRequestEvent event);
    void publishRestaurantRequest(RestaurantRequestEvent event);
    void publishPaymentRollback(PaymentRollbackEvent event);
}