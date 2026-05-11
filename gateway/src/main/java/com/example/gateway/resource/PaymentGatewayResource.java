package com.example.gateway.resource;

import com.example.gateway.client.PaymentServiceClient;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Set;

@Path("/api/payment")
@Produces(MediaType.TEXT_PLAIN)
public class PaymentGatewayResource {

    private static final Set<String> BLOCKED_HEADERS = Set.of(
            "transfer-encoding", "content-length", "host", "connection", "x-correlation-id"
    );

    @Inject
    @RestClient
    PaymentServiceClient paymentServiceClient;

    @GET
    @Path("/delay")
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDelay() {
        return forward(paymentServiceClient.getDelay());
    }

    @PUT
    @Path("/delay")
    @RolesAllowed("admin")
    public Response setDelay(@QueryParam("seconds") int seconds) {
        return forward(paymentServiceClient.setDelay(seconds));
    }

    private Response forward(Response downstream) {
        String body = downstream.hasEntity() ? downstream.readEntity(String.class) : null;
        Response.ResponseBuilder builder = Response.status(downstream.getStatus());
        if (body != null) builder.entity(body);
        downstream.getHeaders().forEach((k, values) -> {
            if (!BLOCKED_HEADERS.contains(k.toLowerCase())) values.forEach(v -> builder.header(k, v));
        });
        return builder.build();
    }
}