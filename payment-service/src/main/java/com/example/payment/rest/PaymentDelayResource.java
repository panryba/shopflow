package com.example.payment.rest;

import com.example.payment.infrastructure.messaging.PaymentEventConsumer;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/payment")
public class PaymentDelayResource {

    @Inject
    PaymentEventConsumer consumer;

    @GET
    @Path("/delay")
    @Produces(MediaType.APPLICATION_JSON)
    public int getDelay() {
        return consumer.getDelaySeconds();
    }

    @PUT
    @Path("/delay")
    public Response setDelay(@QueryParam("seconds") int seconds) {
        consumer.setDelaySeconds(seconds);
        return Response.ok("Payment delay set to: " + seconds + "s").build();
    }
}