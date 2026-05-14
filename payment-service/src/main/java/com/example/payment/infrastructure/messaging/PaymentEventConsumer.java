package com.example.payment.infrastructure.messaging;

import com.example.payment.infrastructure.metrics.PaymentMetrics;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.MDC;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class PaymentEventConsumer {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @ConfigProperty(name = "app.payment.failure-threshold")
    double failureThreshold;

    @Inject
    PaymentMetrics metrics;

    @ConfigProperty(name = "app.payment.crash", defaultValue = "false")
    boolean crash;

    private volatile int delaySeconds = 0;

    public int getDelaySeconds() { return delaySeconds; }

    public void setDelaySeconds(int seconds) {
        Log.infof("Payment delay changed: %ds", seconds);
        this.delaySeconds = seconds;
    }

    public boolean isCrash() { return crash; }

    public void setCrash(boolean crash) {
        Log.infof("Payment crash mode changed: crash=%s", crash);
        this.crash = crash;
    }

    private void sleep() {
        if (delaySeconds > 0) try { Thread.sleep(delaySeconds * 1000L); } catch (InterruptedException ignored) {}
    }

    @Channel("payment-completed")
    Emitter<com.example.order.events.avro.PaymentCompletedEvent> successEmitter;

    @Channel("payment-failed")
    Emitter<com.example.order.events.avro.PaymentFailedEvent> failedEmitter;

    @Channel("payment-rollback-completed")
    Emitter<com.example.order.events.avro.PaymentRollbackCompletedEvent> rollbackCompletedEmitter;

    @Incoming("payment-request")
    @io.smallrye.reactive.messaging.annotations.Blocking
    public CompletionStage<Void> process(Message<com.example.order.events.avro.PaymentRequestEvent> message) {
        try {
            if (crash) throw new RuntimeException("Simulated consumer crash");
            sleep();
            var avro = message.getPayload();
            boolean success = processPayment(avro.getAmount().toString());
            String orderId = avro.getOrderId().toString();
            String correlationId = avro.getCorrelationId().toString();
            MDC.put("correlationId", correlationId);
            MDC.put("orderId", orderId);

            if (success) {
                metrics.accepted();
                Log.infof("Payment accepted");
                successEmitter.send(Message.of(
                        com.example.order.events.avro.PaymentCompletedEvent.newBuilder()
                                .setEventId(UUID.randomUUID().toString())
                                .setOrderId(orderId)
                                .setCorrelationId(correlationId)
                                .build())
                        .addMetadata(key(orderId, correlationId)));
            } else {
                metrics.rejected();
                Log.infof("Payment rejected reason=Insufficient funds");
                failedEmitter.send(Message.of(
                        com.example.order.events.avro.PaymentFailedEvent.newBuilder()
                                .setEventId(UUID.randomUUID().toString())
                                .setOrderId(orderId)
                                .setReason("Insufficient funds")
                                .setCorrelationId(correlationId)
                                .build())
                        .addMetadata(key(orderId, correlationId)));
            }
            return message.ack();
        } catch (Exception e) {
            Log.errorf("Payment consumer failure: %s", e.getMessage());
            return message.nack(e);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("orderId");
        }
    }

    @Incoming("payment-rollback")
    @io.smallrye.reactive.messaging.annotations.Blocking
    public CompletionStage<Void> rollback(Message<com.example.order.events.avro.PaymentRollbackEvent> message) {
        try {
            sleep();
            var avro = message.getPayload();
            String orderId = avro.getOrderId().toString();
            String correlationId = avro.getCorrelationId().toString();

            MDC.put("correlationId", correlationId);
            MDC.put("orderId", orderId);
            metrics.rolledBack();
            Log.infof("Payment rollback completed");

            rollbackCompletedEmitter.send(Message.of(
                    com.example.order.events.avro.PaymentRollbackCompletedEvent.newBuilder()
                            .setEventId(UUID.randomUUID().toString())
                            .setOrderId(orderId)
                            .setCorrelationId(correlationId)
                            .build())
                    .addMetadata(key(orderId, correlationId)));

            return message.ack();
        } catch (Exception e) {
            return message.nack(e);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("orderId");
        }
    }

    private boolean processPayment(String amountStr) {
        return new BigDecimal(amountStr).doubleValue() < failureThreshold;
    }

    private OutgoingKafkaRecordMetadata<String> key(String orderId, String correlationId) {
        return OutgoingKafkaRecordMetadata.<String>builder()
                .withKey(orderId)
                .withHeaders(new RecordHeaders()
                        .add(CORRELATION_ID_HEADER, correlationId.getBytes(StandardCharsets.UTF_8)))
                .build();
    }
}
