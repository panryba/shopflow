package com.example.order.presentation;

import com.example.order.application.port.input.OrderUseCase;
import com.example.order.application.saga.OrderSagaOrchestrator;
import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.presentation.dto.CreateOrderRequest;
import com.example.order.presentation.dto.OrderResponse;
import com.example.order.presentation.mapper.OrderPresentationMapper;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.util.List;
import java.util.UUID;

@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    OrderUseCase service;

    @Inject
    OrderSagaOrchestrator orchestrator;

    @Inject
    OrderPresentationMapper mapper;

    @POST
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public Response create(@Valid CreateOrderRequest request) {
        UUID orderId = orchestrator.start(request);
        Log.infof("Order accepted orderId=%s", orderId);
        return Response.accepted()
                .header("Location", "/orders/" + orderId)
                .build();
    }

    @PUT
    @Path("/{id}/cancel")
    @Retry(maxRetries = 3, delay = 200, jitter = 200,
            retryOn = OptimisticLockException.class,
            abortOn = NotFoundException.class)
    public Response cancel(@PathParam("id") UUID id) {
        Log.infof("Order cancel requested orderId=%s", id);
        service.cancel(new OrderId(id));
        Order order = service.findById(new OrderId(id));
        return Response.ok(mapper.toResponse(order)).build();
    }

    @GET
    @Retry(maxRetries = 3, delay = 200, jitter = 200, retryOn = PersistenceException.class)
    public List<OrderResponse> getAll() {
        return service.findAllOrders().stream().map(mapper::toResponse).toList();
    }

    @GET
    @Path("/{id}")
    @Retry(maxRetries = 3, delay = 200, jitter = 200,
            retryOn = PersistenceException.class,
            abortOn = NotFoundException.class)
    public OrderResponse getById(@PathParam("id") UUID id) {
        Order order = service.findById(new OrderId(id));
        return mapper.toResponse(order);
    }
}