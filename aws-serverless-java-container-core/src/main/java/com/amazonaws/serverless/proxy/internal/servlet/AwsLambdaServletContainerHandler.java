/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazonaws.serverless.proxy.internal.*;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Abstract extension of the code <code>LambdaContainerHandler</code> object that adds protected variables for the
 * <code>ServletContext</code> and <code>FilterChainManager</code>. This object should be extended by the framework-specific
 * implementations that want to support the servlet 3.1 specs.
 * @param <RequestType>
 * @param <ResponseType>
 * @param <ContainerRequestType>
 * @param <ContainerResponseType>
 */
public abstract class AwsLambdaServletContainerHandler<RequestType, ResponseType,
        ContainerRequestType extends HttpServletRequest,
        ContainerResponseType extends HttpServletResponse>
        extends LambdaContainerHandler<RequestType, ResponseType, ContainerRequestType, ContainerResponseType> {

    //-------------------------------------------------------------
    // Variables - Protected
    //-------------------------------------------------------------

    private FilterChainManager filterChainManager;

    //-------------------------------------------------------------
    // Variables - Protected
    //-------------------------------------------------------------

    protected ServletContext servletContext;
    protected StartupHandler startupHandler;


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    protected AwsLambdaServletContainerHandler(RequestReader<RequestType, ContainerRequestType> requestReader,
                                     ResponseWriter<ContainerResponseType, ResponseType> responseWriter,
                                     SecurityContextWriter<RequestType> securityContextWriter,
                                     ExceptionHandler<ResponseType> exceptionHandler) {
        super(requestReader, responseWriter, securityContextWriter, exceptionHandler);
    }

    //-------------------------------------------------------------
    // Methods - Protected
    //-------------------------------------------------------------

    /**
     * Sets the ServletContext in the handler and initialized a new <code>FilterChainManager</code>
     * @param context An initialized ServletContext
     */
    protected void setServletContext(final ServletContext context) {
        servletContext = context;
        // We assume custom implementations of the RequestWriter for HttpServletRequest will reuse
        // the existing AwsServletContext object since it has no dependencies other than the Lambda context
        filterChainManager = new AwsFilterChainManager((AwsServletContext)context);
    }

    /**
     * Applies the filter chain in the request lifecycle
     * @param request The Request object. This must be an implementation of HttpServletRequest
     * @param response The response object. This must be an implementation of HttpServletResponse
     */
    protected void doFilter(ContainerRequestType request, ContainerResponseType response) throws IOException, ServletException {
        FilterChainHolder chain = filterChainManager.getFilterChain(request);
        chain.doFilter(request, response);
    }

    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    /**
     * Returns the current ServletContext. If the framework implementation does not set the value for
     * servlet context this method will return null.
     * @return The initialized servlet context if the framework-specific implementation requires one, otherwise null
     */
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * You can use the <code>setStartupHandler</code> to intercept the ServletContext as the Spring application is
     * initialized and inject custom values. The StartupHandler is called after the <code>onStartup</code> method
     * of the <code>LambdaSpringApplicationinitializer</code> implementation. For example, you can use this method to
     * add custom filters to the servlet context:
     *
     * <pre>
     * {@code
     *      handler = SpringLambdaContainerHandler.getAwsProxyHandler(EchoSpringAppConfig.class);
     *      handler.setStartupHandler(c -> {
     *      // the "c" parameter to this function is the initialized servlet context
     *      c.addFilter("CustomHeaderFilter", CustomHeaderFilter.class);
     *      });
     * }
     * </pre>
     * @param h A lambda expression that implements the <code>StartupHandler</code> functional interface
     */
    public void setStartupHandler(final StartupHandler h) {
        startupHandler = h;
    }

    public interface StartupHandler {
        void onStartup(ServletContext context);
    }
}
