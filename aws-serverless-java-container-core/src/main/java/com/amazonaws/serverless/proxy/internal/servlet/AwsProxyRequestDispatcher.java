package com.amazonaws.serverless.proxy.internal.servlet;


import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.amazonaws.serverless.proxy.RequestReader.API_GATEWAY_EVENT_PROPERTY;
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
        if (!namedDispatcher && !target.startsWith("/")) {
            throw new UnsupportedOperationException("Only dispatchers with absolute paths are supported");
        }

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
            lambdaContainerHandler.doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, lambdaContainerHandler.getServlet());
            return;
        }

        servletRequest.setAttribute(DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.FORWARD);
        setRequestPath(servletRequest, dispatchTo);
        lambdaContainerHandler.doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, lambdaContainerHandler.getServlet());
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
        lambdaContainerHandler.doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, lambdaContainerHandler.getServlet());
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

        log.debug("Request is not an AwsProxyHttpServletRequest, attempting to extract the proxy event type");
        if (req.getAttribute(API_GATEWAY_EVENT_PROPERTY) == null || !(req.getAttribute(API_GATEWAY_EVENT_PROPERTY) instanceof AwsProxyRequest)) {
            throw new IllegalStateException("ServletRequest object does not contain API Gateway event");
        }
        ((AwsProxyRequest)req.getAttribute(API_GATEWAY_EVENT_PROPERTY)).setPath(dispatchTo);
    }
}
