package com.amazonaws.serverless.proxy.jersey.providers;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;

import java.io.IOException;


public class ServletRequestFilter implements ContainerRequestFilter {
    public static final String FILTER_ATTRIBUTE_NAME = "ServletFilter";
    public static final String FILTER_ATTRIBUTE_VALUE = "done";

    @Context HttpServletRequest request;

    public void filter(ContainerRequestContext ctx) throws IOException {
        request.setAttribute(FILTER_ATTRIBUTE_NAME, FILTER_ATTRIBUTE_VALUE);
    }

}