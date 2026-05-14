package com.example.gateway.resource;

import com.example.gateway.client.InventoryServiceClient;
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

@Path("/api/inventory")
@Produces(MediaType.TEXT_PLAIN)
public class InventoryGatewayResource {

    private static final Set<String> BLOCKED_HEADERS = Set.of(
            "transfer-encoding",
            "content-length",
            "host",
            "connection",
            "x-correlation-id"
    );

    @Inject
    @RestClient
    InventoryServiceClient inventoryServiceClient;

    @GET
    @Path("/mode")
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMode() {
        return forward(inventoryServiceClient.getMode());
    }

    @PUT
    @Path("/mode")
    @RolesAllowed("admin")
    public Response setMode(@QueryParam("accept") boolean accept) {
        return forward(inventoryServiceClient.setMode(accept));
    }

    @GET
    @Path("/delay")
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDelay() {
        return forward(inventoryServiceClient.getDelay());
    }

    @PUT
    @Path("/delay")
    @RolesAllowed("admin")
    public Response setDelay(@QueryParam("seconds") int seconds) {
        return forward(inventoryServiceClient.setDelay(seconds));
    }

    @GET
    @Path("/crash")
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCrash() {
        return forward(inventoryServiceClient.getCrash());
    }

    @PUT
    @Path("/crash")
    @RolesAllowed("admin")
    public Response setCrash(@QueryParam("enabled") boolean enabled) {
        return forward(inventoryServiceClient.setCrash(enabled));
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
