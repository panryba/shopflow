package com.example.inventory.rest;

import com.example.inventory.infrastructure.messaging.InventoryEventConsumer;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/inventory")
public class InventoryModeResource {

    @Inject
    InventoryEventConsumer consumer;

    @GET
    @Path("/mode")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean getMode() {
        return consumer.isAccepted();
    }

    @PUT
    @Path("/mode")
    public Response setMode(@QueryParam("accept") boolean accept) {
        consumer.setAccepted(accept);
        return Response.ok("Inventory acceptance set to: " + accept).build();
    }
}