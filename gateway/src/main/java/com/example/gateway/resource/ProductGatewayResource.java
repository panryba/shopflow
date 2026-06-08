package com.example.gateway.resource;

import com.example.gateway.client.ProductServiceClient;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestForm;

@Authenticated
@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductGatewayResource {

    @Inject
    @RestClient
    ProductServiceClient productServiceClient;

    @Inject
    GatewayResponseForwarder forwarder;

    @GET
    public Response getAll() {
        return forwarder.forward(productServiceClient.getAll());
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("admin")
    public Response importProducts(ProductUploadForm form) {
        ProductServiceClient.ProductImportForm clientForm = new ProductServiceClient.ProductImportForm();
        clientForm.file = form.file;
        return forwarder.forward(productServiceClient.importProducts(clientForm));
    }

    public static class ProductUploadForm {
        @RestForm("file")
        public byte[] file;
    }
}