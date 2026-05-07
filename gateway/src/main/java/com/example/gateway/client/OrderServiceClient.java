package com.example.gateway.client;

import com.example.gateway.infrastructure.filter.OutgoingCorrelationIdFilter;
import com.example.gateway.infrastructure.filter.OutgoingJwtFilter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@RegisterRestClient(configKey = "order-service")
@RegisterProvider(OutgoingCorrelationIdFilter.class)
@RegisterProvider(OutgoingJwtFilter.class)
@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface OrderServiceClient {

    @POST
    @CircuitBreaker(requestVolumeThreshold = 10, delay = 5000, successThreshold = 2)
    Response create(String body);

    @GET
    @Retry(maxRetries = 3, delay = 200, abortOn = WebApplicationException.class)
    @CircuitBreaker(requestVolumeThreshold = 10, delay = 5000, successThreshold = 2)
    Response getAll();

    @GET
    @Path("/{id}")
    @Retry(maxRetries = 3, delay = 200, abortOn = WebApplicationException.class)
    @CircuitBreaker(requestVolumeThreshold = 10, delay = 5000, successThreshold = 2)
    Response getById(@PathParam("id") UUID id);

    @PUT
    @Path("/{id}/cancel")
    @Retry(maxRetries = 3, delay = 200, abortOn = WebApplicationException.class)
    @CircuitBreaker(requestVolumeThreshold = 10, delay = 5000, successThreshold = 2)
    Response cancel(@PathParam("id") UUID id);
}