package com.example.gateway.resource;

import com.example.gateway.client.InventoryServiceClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/inventory")
@Produces(MediaType.TEXT_PLAIN)
public class InventoryGatewayResource {

    @Inject
    @RestClient
    InventoryServiceClient inventoryServiceClient;

    @PUT
    @Path("/mode")
    public Response setMode(@QueryParam("accept") boolean accept) {
        return inventoryServiceClient.setMode(accept);
    }
}
