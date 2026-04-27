package com.example.gateway.resource;

import com.example.gateway.infrastructure.observability.CorrelationIdProvider;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GatewayExceptionMapper implements ExceptionMapper<Exception> {

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public Response toResponse(Exception exception) {
        String correlationId = correlationIdProvider.get();

        if (exception instanceof WebApplicationException wae) {
            Response downstream = wae.getResponse();
            Log.warnf("Downstream returned %d [corrId=%s]", downstream.getStatus(), correlationId);
            return Response.status(downstream.getStatus())
                    .entity(downstream.getEntity())
                    .build();
        }

        if (exception instanceof ProcessingException) {
            Log.errorf(exception, "Downstream service unreachable [corrId=%s]", correlationId);
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new GatewayErrorResponse(502, "SERVICE_UNAVAILABLE",
                            "Downstream service temporarily unavailable", correlationId))
                    .build();
        }

        Log.errorf(exception, "Unexpected gateway error [corrId=%s]", correlationId);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new GatewayErrorResponse(500, "INTERNAL_ERROR",
                        "Unexpected error", correlationId))
                .build();
    }
}
