package com.amazonaws.serverless.proxy.jersey.providers;


import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


@Provider
public class CustomExceptionMapper implements ExceptionMapper<UnsupportedOperationException> {

    public CustomExceptionMapper() {
        System.out.println("Starting custom exception mapper");
    }

    @Inject
    public javax.inject.Provider<HttpServletRequest> request;

    @Override
    public Response toResponse(UnsupportedOperationException throwable) {
        if (request == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            System.out.println("Request uri: " + request.get().getRequestURI());
            return Response.ok(throwable.getMessage()).status(Response.Status.NOT_IMPLEMENTED).build();
        }
    }
}
