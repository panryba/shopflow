package com.example.order.infrastructure.inbox;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

@ApplicationScoped
public class InboxRepository implements PanacheRepositoryBase<InboxEvent, String> {

    public long deleteOlderThan(Instant cutoff) {
        return delete("status in (?1, ?2) and receivedAt < ?3",
                InboxEvent.Status.PROCESSED,
                InboxEvent.Status.FAILED,
                cutoff);
    }
}
