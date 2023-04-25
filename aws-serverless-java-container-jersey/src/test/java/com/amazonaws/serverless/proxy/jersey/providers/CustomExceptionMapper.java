package com.amazonaws.serverless.proxy.jersey.providers;


import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;


@Provider
public class CustomExceptionMapper implements ExceptionMapper<UnsupportedOperationException> {

    public CustomExceptionMapper() {

    }

    @Inject
    public jakarta.inject.Provider<HttpServletRequest> request;

    @Override
    public Response toResponse(UnsupportedOperationException throwable) {
        if (request == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok(throwable.getMessage()).status(Response.Status.NOT_IMPLEMENTED).build();
        }
    }
}
