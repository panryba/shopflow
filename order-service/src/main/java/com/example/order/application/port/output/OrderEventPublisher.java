package com.example.order.application.port.output;

import com.example.order.domain.event.PaymentRequestEvent;
import com.example.order.domain.event.PaymentRollbackEvent;
import com.example.order.domain.event.InventoryRequestEvent;

public interface OrderEventPublisher {
    void publishPaymentRequest(PaymentRequestEvent event);
    void publishInventoryRequest(InventoryRequestEvent event);
    void publishPaymentRollback(PaymentRollbackEvent event);
}
