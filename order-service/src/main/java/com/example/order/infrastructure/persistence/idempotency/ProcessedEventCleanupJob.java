package com.example.order.infrastructure.persistence.idempotency;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class ProcessedEventCleanupJob {

    @Inject
    ProcessedEventRepository repository;

    @ConfigProperty(name = "app.idempotency.retention-days")
    int retentionDays;

    @Scheduled(every = "1h")
    void cleanup() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        long deleted = repository.deleteOlderThan(cutoff);
        Log.infov("Cleanup removed: {0,number,#}", deleted);
    }
}