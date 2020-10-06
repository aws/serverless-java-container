/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.serverless.proxy.jersey;


import com.amazonaws.serverless.proxy.*;
import com.amazonaws.serverless.proxy.internal.servlet.*;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.jersey.suppliers.AwsProxyServletContextSupplier;
import com.amazonaws.serverless.proxy.jersey.suppliers.AwsProxyServletRequestSupplier;
import com.amazonaws.serverless.proxy.jersey.suppliers.AwsProxyServletResponseSupplier;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;

import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Application;

import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;


/**
 * Jersey-specific implementation of the <code>LambdaContainerHandler</code> interface. Given a Jax-Rs application
 * starts Jersey's <code>ApplicationHandler</code> and proxies requests and responses using the RequestReader and
 * ResponseWriter objects. The reader and writer objects are inherited from the <code>BaseLambdaContainerHandler</code>
 * object.
 *
 * <pre>
 * {@code
 *   public class LambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
 *     private ResourceConfig jerseyApplication = new ResourceConfig().packages("your.app.package");
 *     private JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> container = JerseyLambdaContainerHandler.getAwsProxyHandler(jerseyApplication);
 *
 *     public AwsProxyResponse handleRequest(AwsProxyRequest awsProxyRequest, Context context) {
 *       return container.proxy(awsProxyRequest, context);
 *     }
 *   }
 * }
 * </pre>
 *
 * @see com.amazonaws.serverless.proxy.internal.LambdaContainerHandler
 *
 * @param <RequestType> The type for the incoming Lambda event
 * @param <ResponseType> The type for Lambda's return value
 */
public class JerseyLambdaContainerHandler<RequestType, ResponseType> extends AwsLambdaServletContainerHandler<RequestType, ResponseType, HttpServletRequest, AwsHttpServletResponse> {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private JerseyHandlerFilter jerseyFilter;
    private boolean initialized;

    //-------------------------------------------------------------
    // Methods - Public - Static
    //-------------------------------------------------------------

    /**
     * Returns an initialized <code>JerseyLambdaContainerHandler</code> that includes <code>RequestReader</code> and
     * <code>ResponseWriter</code> objects for the <code>AwsProxyRequest</code> and <code>AwsProxyResponse</code>
     * objects.
     *
     * @param jaxRsApplication A configured Jax-Rs application object. For Jersey apps this can be the default
     *                         <code>ResourceConfig</code> object
     * @return A <code>JerseyLambdaContainerHandler</code> object
     */
    public static JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(Application jaxRsApplication) {
        JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> newHandler = new JerseyLambdaContainerHandler<>(
                AwsProxyRequest.class,
                AwsProxyResponse.class,
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                jaxRsApplication);
        newHandler.initialize();
        return newHandler;
    }

    /**
     * Returns an initialized <code>JerseyLambdaContainerHandler</code> that includes <code>RequestReader</code> and
     * <code>ResponseWriter</code> objects for the <code>HttpApiV2ProxyRequest</code> and <code>AwsProxyResponse</code>
     * objects.
     *
     * @param jaxRsApplication A configured Jax-Rs application object. For Jersey apps this can be the default
     *                         <code>ResourceConfig</code> object
     * @return A <code>JerseyLambdaContainerHandler</code> object
     */
    public static JerseyLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> getHttpApiV2ProxyHandler(Application jaxRsApplication) {
        JerseyLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> newHandler = new JerseyLambdaContainerHandler<>(
                HttpApiV2ProxyRequest.class,
                AwsProxyResponse.class,
                new AwsHttpApiV2HttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(true),
                new AwsHttpApiV2SecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                jaxRsApplication);
        newHandler.initialize();
        return newHandler;
    }


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    /**
     * Private constructor for a LambdaContainer. Sets the application object, sets the ApplicationHandler,
     * and initializes the application using the <code>onStartup</code> method.
     * @param requestTypeClass The class for the expected event type
     * @param responseTypeClass The class for the output type
     * @param requestReader A request reader instance
     * @param responseWriter A response writer instance
     * @param securityContextWriter A security context writer object
     * @param exceptionHandler An exception handler
     * @param jaxRsApplication The JaxRs application
     */
    public JerseyLambdaContainerHandler(Class<RequestType> requestTypeClass,
                                        Class<ResponseType> responseTypeClass,
                                        RequestReader<RequestType, HttpServletRequest> requestReader,
                                        ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                        SecurityContextWriter<RequestType> securityContextWriter,
                                        ExceptionHandler<ResponseType> exceptionHandler,
                                        Application jaxRsApplication) {

        super(requestTypeClass, responseTypeClass, requestReader, responseWriter, securityContextWriter, exceptionHandler);
        Timer.start("JERSEY_CONTAINER_CONSTRUCTOR");
        initialized = false;
        if (jaxRsApplication instanceof ResourceConfig) {
            ((ResourceConfig)jaxRsApplication).register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bindFactory(AwsProxyServletContextSupplier.class)
                            .proxy(true)
                            .proxyForSameScope(true)
                            .to(ServletContext.class)
                            .in(RequestScoped.class);
                    bindFactory(AwsProxyServletRequestSupplier.class)
                            .proxy(true)
                            .proxyForSameScope(true)
                            .to(HttpServletRequest.class)
                            .in(RequestScoped.class);
                    bindFactory(AwsProxyServletResponseSupplier.class)
                            .proxy(true)
                            .proxyForSameScope(true)
                            .to(HttpServletResponse.class)
                            .in(RequestScoped.class);
                }
            });
        }

        this.jerseyFilter = new JerseyHandlerFilter(jaxRsApplication);
        Timer.stop("JERSEY_CONTAINER_CONSTRUCTOR");
    }

    //-------------------------------------------------------------
    // Methods - Implementation
    //-------------------------------------------------------------

    @Override
    protected void handleRequest(HttpServletRequest httpServletRequest, AwsHttpServletResponse httpServletResponse, Context lambdaContext)
            throws Exception {
        // we retain the initialized property for backward compatibility
        if (!initialized) {
            initialize();
        }
        Timer.start("JERSEY_HANDLE_REQUEST");

        if (AwsHttpServletRequest.class.isAssignableFrom(httpServletRequest.getClass())) {
            ((AwsHttpServletRequest)httpServletRequest).setServletContext(getServletContext());
        }

        doFilter(httpServletRequest, httpServletResponse, null);
        Timer.stop("JERSEY_HANDLE_REQUEST");
    }

    @Override
    protected AwsHttpServletResponse getContainerResponse(HttpServletRequest request, CountDownLatch latch) {
        return new AwsHttpServletResponse(request, latch);
    }

    @Override
    public void initialize() {
        Timer.start("JERSEY_COLD_START_INIT");

        // manually add the spark filter to the chain. This should the last one and match all uris
        FilterRegistration.Dynamic jerseyFilterReg = getServletContext().addFilter("JerseyFilter", jerseyFilter);
        jerseyFilterReg.addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.INCLUDE, DispatcherType.FORWARD),
                true, "/*"
        );

        Timer.stop("JERSEY_COLD_START_INIT");
        initialized = true;
    }


    public InjectionManager getInjectionManager() {
        if (!initialized) {
            initialize();
        }
        return jerseyFilter.getApplicationHandler().getInjectionManager();
    }

    public Servlet getServlet() {
        return null;
    }
}
