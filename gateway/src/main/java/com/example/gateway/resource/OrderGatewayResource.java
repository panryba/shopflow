package com.example.gateway.resource;

import com.example.gateway.client.OrderServiceClient;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
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

import java.util.Set;
import java.util.UUID;

@Authenticated
@Path("/api/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderGatewayResource {

    private static final Set<String> BLOCKED_HEADERS = Set.of(
            "transfer-encoding",
            "content-length",
            "host",
            "connection",
            "x-correlation-id"
    );

    @Inject
    @RestClient
    OrderServiceClient orderServiceClient;

    @POST
    public Response create(String body) {
        return forward(orderServiceClient.create(body));
    }

    @GET
    public Response getAll() {
        return forward(orderServiceClient.getAll());
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") UUID id) {
        return forward(orderServiceClient.getById(id));
    }

    @PUT
    @Path("/{id}/cancel")
    public Response cancel(@PathParam("id") UUID id) {
        return forward(orderServiceClient.cancel(id));
    }

    private Response forward(Response downstream) {
        String body = downstream.hasEntity()
                ? downstream.readEntity(String.class)
                : null;

        Response.ResponseBuilder builder = Response.status(downstream.getStatus());

        if (body != null) {
            builder.entity(body);
        }

        downstream.getHeaders().forEach((k, values) -> {
            if (!BLOCKED_HEADERS.contains(k.toLowerCase())) {
                values.forEach(v -> builder.header(k, v));
            }
        });

        return builder.build();
    }
}
