package com.example.gateway.infrastructure.filter;

import com.example.gateway.infrastructure.observability.CorrelationIdProvider;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
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
@ApplicationScoped
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
        Log.infof("%s %s", requestContext.getMethod(), requestContext.getUriInfo().getRequestUri().getPath());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String id = correlationIdProvider.get();
        if (id != null) {
            responseContext.getHeaders().putSingle(HEADER, id);
        }
        Log.infof("%s %s → %d", requestContext.getMethod(), requestContext.getUriInfo().getRequestUri().getPath(), responseContext.getStatus());
        correlationIdProvider.clear();
    }
}
