package com.example.gateway.resource;

import com.example.gateway.client.OrderServiceClient;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.UUID;

@Path("/api/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderGatewayResource {

    @Inject
    @RestClient
    OrderServiceClient orderServiceClient;

    @POST
    public Response create(JsonObject body) {
        return orderServiceClient.create(body);
    }

    @GET
    public Response getAll() {
        return orderServiceClient.getAll();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") UUID id) {
        return orderServiceClient.getById(id);
    }

    @PUT
    @Path("/{id}/cancel")
    public Response cancel(@PathParam("id") UUID id) {
        return orderServiceClient.cancel(id);
    }
}
