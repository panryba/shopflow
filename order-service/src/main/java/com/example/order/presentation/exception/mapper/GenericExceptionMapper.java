package com.example.order.presentation.exception.mapper;

import com.example.order.infrastructure.observability.CorrelationIdProvider;
import com.example.order.presentation.exception.ErrorResponse;
import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        String correlationId = CorrelationIdProvider.get();
        Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
        Log.errorf(exception, "Unhandled exception [correlationId=%s]", correlationId);
        return Response.status(status)
                .entity(ErrorResponse.of(status.getStatusCode(), "INTERNAL_ERROR",
                        "Unexpected error occurred", correlationId))
                .build();
    }
}