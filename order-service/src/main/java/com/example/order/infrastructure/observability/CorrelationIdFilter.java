package com.example.order.infrastructure.observability;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;

@Provider
@PreMatching
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String HEADER = "X-Correlation-ID";

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String id = requestContext.getHeaderString(HEADER);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        correlationIdProvider.set(id);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String id = correlationIdProvider.get();
        responseContext.getHeaders().add(HEADER, id);
        correlationIdProvider.clear();
    }
}
