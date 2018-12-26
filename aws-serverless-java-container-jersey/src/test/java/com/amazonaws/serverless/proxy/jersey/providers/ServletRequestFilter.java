package com.amazonaws.serverless.proxy.jersey.providers;


import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;

import java.io.IOException;


public class ServletRequestFilter implements ContainerRequestFilter {
    public static final String FILTER_ATTRIBUTE_NAME = "ServletFilter";
    public static final String FILTER_ATTRIBUTE_VALUE = "done";

    @Context HttpServletRequest request;

    public void filter(ContainerRequestContext ctx) throws IOException {
        request.setAttribute(FILTER_ATTRIBUTE_NAME, FILTER_ATTRIBUTE_VALUE);
    }

}