package com.example.order.infrastructure.persistence.idempotency;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class ProcessedEventRepository implements PanacheRepository<ProcessedEventEntity> {

    @Transactional
    public void save(String eventId) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(eventId);
        persist(entity);
    }

    @Transactional
    public long deleteOlderThan(Instant cutoff) {
        return delete("processedAt < ?1", cutoff);
    }
}