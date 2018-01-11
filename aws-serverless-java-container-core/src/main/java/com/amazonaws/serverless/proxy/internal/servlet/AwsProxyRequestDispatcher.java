package com.amazonaws.serverless.proxy.internal.servlet;


import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;


/**
 * Default <code>RequestDispatcher</code> implementation for the <code>AwsProxyHttpServletRequest</code> type. A new
 * instance of this object is created each time a framework gets the RequestDispatcher from a servlet request. Behind
 * the scenes, this object uses the <code>AwsLambdaServletContainerHandler</code> to send FORWARD and INCLUDE requests
 * to the framework.
 */
public class AwsProxyRequestDispatcher implements RequestDispatcher {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private String dispatchPath;
    private AwsLambdaServletContainerHandler lambdaContainerHandler;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------


    public AwsProxyRequestDispatcher(final String path, final AwsLambdaServletContainerHandler handler) {
        if (!path.startsWith("/")) {
            throw new UnsupportedOperationException("Only dispatchers with absolute paths are supported");
        }

        dispatchPath = path;
        lambdaContainerHandler = handler;
    }

    //-------------------------------------------------------------
    // Implementation - RequestDispatcher
    //-------------------------------------------------------------


    @Override
    @SuppressWarnings("unchecked")
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse)
            throws ServletException, IOException {
        if (!(servletRequest instanceof AwsProxyHttpServletRequest)) {
            throw new IOException("Invalid request type: " + servletRequest.getClass().getSimpleName() + ". Only AwsProxyHttpServletRequest is supported");
        }

        if (lambdaContainerHandler == null) {
            throw new IOException("Null container handler in dispatcher");
        }

        ((AwsProxyHttpServletRequest) servletRequest).setDispatcherType(DispatcherType.FORWARD);
        ((AwsProxyHttpServletRequest) servletRequest).getAwsProxyRequest().setPath(dispatchPath);

        lambdaContainerHandler.forward((HttpServletRequest)servletRequest, (HttpServletResponse)servletResponse);
    }


    @Override
    @SuppressWarnings("unchecked")
    public void include(ServletRequest servletRequest, ServletResponse servletResponse)
            throws ServletException, IOException {
        if (!(servletRequest instanceof AwsProxyHttpServletRequest)) {
            throw new IOException("Invalid request type: " + servletRequest.getClass().getSimpleName() + ". Only AwsProxyHttpServletRequest is supported");
        }

        if (lambdaContainerHandler == null) {
            throw new IOException("Null container handler in dispatcher");
        }

        ((AwsProxyHttpServletRequest) servletRequest).setDispatcherType(DispatcherType.INCLUDE);
        ((AwsProxyHttpServletRequest) servletRequest).getAwsProxyRequest().setPath(dispatchPath);

        lambdaContainerHandler.include((HttpServletRequest)servletRequest, (HttpServletResponse)servletResponse);
    }
}
