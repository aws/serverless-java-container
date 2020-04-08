/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.amazonaws.serverless.proxy.RequestReader.API_GATEWAY_EVENT_PROPERTY;
import static com.amazonaws.serverless.proxy.RequestReader.HTTP_API_EVENT_PROPERTY;
import static com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletRequest.DISPATCHER_TYPE_ATTRIBUTE;

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
    private static final Logger log = LoggerFactory.getLogger(AwsHttpSession.class);
    private String dispatchTo;
    private boolean isNamedDispatcher;
    private AwsLambdaServletContainerHandler lambdaContainerHandler;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------


    public AwsProxyRequestDispatcher(final String target, final boolean namedDispatcher, final AwsLambdaServletContainerHandler handler) {
        isNamedDispatcher = namedDispatcher;
        dispatchTo = target;
        lambdaContainerHandler = handler;
    }

    //-------------------------------------------------------------
    // Implementation - RequestDispatcher
    //-------------------------------------------------------------


    @Override
    @SuppressWarnings("unchecked")
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse)
            throws ServletException, IOException {
        if (lambdaContainerHandler == null) {
            throw new IllegalStateException("Null container handler in dispatcher");
        }
        if (servletResponse.isCommitted()) {
            throw new IllegalStateException("Cannot forward request with committed response");
        }

        try {
            // Reset any output that has been buffered, but keep headers/cookies
            servletResponse.resetBuffer();
        } catch (IllegalStateException e) {
            throw e;
        }

        if (isNamedDispatcher) {
            lambdaContainerHandler.doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, getServlet((HttpServletRequest)servletRequest));
            return;
        }

        servletRequest.setAttribute(DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.FORWARD);
        setRequestPath(servletRequest, dispatchTo);
        lambdaContainerHandler.doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, getServlet((HttpServletRequest)servletRequest));
    }


    @Override
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings("SERVLET_QUERY_STRING")
    public void include(ServletRequest servletRequest, ServletResponse servletResponse)
            throws ServletException, IOException {
        if (lambdaContainerHandler == null) {
            throw new IllegalStateException("Null container handler in dispatcher");
        }
        if (servletResponse.isCommitted()) {
            throw new IllegalStateException("Cannot forward request with committed response");
        }
        servletRequest.setAttribute(DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.INCLUDE);
        if (!isNamedDispatcher) {
            servletRequest.setAttribute("javax.servlet.include.request_uri", ((HttpServletRequest)servletRequest).getRequestURI());
            servletRequest.setAttribute("javax.servlet.include.context_path", ((HttpServletRequest) servletRequest).getContextPath());
            servletRequest.setAttribute("javax.servlet.include.servlet_path", ((HttpServletRequest) servletRequest).getServletPath());
            servletRequest.setAttribute("javax.servlet.include.path_info", ((HttpServletRequest) servletRequest).getPathInfo());
            servletRequest.setAttribute("javax.servlet.include.query_string",
                    SecurityUtils.encode(SecurityUtils.crlf(((HttpServletRequest) servletRequest).getQueryString())));
            setRequestPath(servletRequest, dispatchTo);
        }
        lambdaContainerHandler.doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, getServlet((HttpServletRequest)servletRequest));
    }

    /**
     * Sets the destination path in the given request. Uses the <code>AwsProxyRequest.setPath</code> method which
     * is in turn read by the <code>HttpServletRequest</code> implementation.
     * @param req The request object to be modified
     * @param destinationPath The new path for the request
     * @throws IllegalStateException If the given request object does not include the <code>API_GATEWAY_EVENT_PROPERTY</code>
     *  attribute or the value for the attribute is not of the correct type: <code>AwsProxyRequest</code>.
     */
    void setRequestPath(ServletRequest req, final String destinationPath) {
        if (req instanceof AwsProxyHttpServletRequest) {
            ((AwsProxyHttpServletRequest) req).getAwsProxyRequest().setPath(dispatchTo);
            return;
        }
        if (req instanceof AwsHttpApiV2ProxyHttpServletRequest) {
            ((AwsHttpApiV2ProxyHttpServletRequest) req).getRequest().setRawPath(destinationPath);
            return;
        }

        log.debug("Request is not an proxy request generated by this library, attempting to extract the proxy event type from the request attributes");
        if (req.getAttribute(API_GATEWAY_EVENT_PROPERTY) != null && req.getAttribute(API_GATEWAY_EVENT_PROPERTY) instanceof AwsProxyRequest) {
            ((AwsProxyRequest)req.getAttribute(API_GATEWAY_EVENT_PROPERTY)).setPath(dispatchTo);
            return;
        }
        if (req.getAttribute(HTTP_API_EVENT_PROPERTY) != null && req.getAttribute(HTTP_API_EVENT_PROPERTY) instanceof HttpApiV2ProxyRequest) {
            ((HttpApiV2ProxyRequest)req.getAttribute(HTTP_API_EVENT_PROPERTY)).setRawPath(destinationPath);
            return;
        }

        throw new IllegalStateException("Could not set new target path for the given ServletRequest object");
    }

    private Servlet getServlet(HttpServletRequest req) {
        return ((AwsServletContext)lambdaContainerHandler.getServletContext()).getServletForPath(req.getPathInfo());
    }
}
