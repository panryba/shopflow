package com.example.inventory.rest;

import com.example.inventory.infrastructure.messaging.InventoryEventConsumer;
import jakarta.inject.Inject;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/inventory")
public class InventoryModeResource {

    @Inject
    InventoryEventConsumer consumer;

    @PUT
    @Path("/mode")
    public Response setMode(@QueryParam("accept") boolean accept) {
        consumer.accepted = accept;
        return Response.ok("Inventory acceptance set to: " + accept).build();
    }
}
