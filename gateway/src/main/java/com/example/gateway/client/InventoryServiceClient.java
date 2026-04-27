package com.example.gateway.client;

import com.example.gateway.infrastructure.filter.OutgoingCorrelationIdFilter;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "com.example.gateway.client.InventoryServiceClient")
@RegisterProvider(OutgoingCorrelationIdFilter.class)
@Path("/inventory")
@Produces(MediaType.TEXT_PLAIN)
public interface InventoryServiceClient {

    @PUT
    @Path("/mode")
    Response setMode(@QueryParam("accept") boolean accept);
}
