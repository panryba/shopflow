package com.example.order.application.saga;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.Instant;

@ApplicationScoped
public class SagaCleanupJob {

    @Inject
    OrderSagaRepository sagaRepository;

    @Scheduled(every = "1h")
    @Transactional
    void cleanup() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(3));
        long deleted = sagaRepository.deleteCompletedOlderThan(cutoff);
        if (deleted > 0) {
            Log.infof("Deleted %d completed saga records", deleted);
        }
    }
}
