package com.example.gateway.client;

import com.example.gateway.infrastructure.filter.OutgoingCorrelationIdFilter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@RegisterRestClient(configKey = "com.example.gateway.client.OrderServiceClient")
@RegisterProvider(OutgoingCorrelationIdFilter.class)
@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface OrderServiceClient {

    @POST
    Response create(String body);

    @GET
    Response getAll();

    @GET
    @Path("/{id}")
    Response getById(@PathParam("id") UUID id);

    @PUT
    @Path("/{id}/cancel")
    Response cancel(@PathParam("id") UUID id);
}