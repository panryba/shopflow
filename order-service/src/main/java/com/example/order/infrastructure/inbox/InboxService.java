package com.example.order.infrastructure.inbox;

import com.example.order.infrastructure.observability.OrderMetrics;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.persistence.PersistenceException;

import java.time.Instant;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;

@ApplicationScoped
public class InboxService {

    @Inject
    InboxRepository repository;

    @Inject
    OrderMetrics metrics;

    @Transactional(REQUIRES_NEW)
    public boolean receive(String eventId, InboxEventType type) {
        try {
            repository.persist(
                    InboxEvent.builder()
                            .eventId(eventId)
                            .eventType(type)
                            .receivedAt(Instant.now())
                            .status(InboxEvent.Status.RECEIVED)
                            .build()
            );
            repository.flush();
            return true;
        } catch (PersistenceException e) {
            metrics.inboxDuplicate();
            return false;
        }
    }

    @Transactional
    public void markProcessed(String eventId) {
        InboxEvent event = repository.findById(eventId);
        event.setStatus(InboxEvent.Status.PROCESSED);
    }

    @Transactional(REQUIRES_NEW)
    public void markFailed(String eventId, String error) {
        InboxEvent event = repository.findById(eventId);
        if (event == null) return;
        event.setStatus(InboxEvent.Status.FAILED);
        event.setErrorMessage(error);
    }
}
