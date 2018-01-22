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


import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.ExceptionHandler;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.ResponseWriter;
import com.amazonaws.serverless.proxy.SecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsLambdaServletContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;

import com.amazonaws.services.lambda.runtime.Context;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
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
public class JerseyLambdaContainerHandler<RequestType, ResponseType> extends AwsLambdaServletContainerHandler<RequestType, ResponseType, AwsProxyHttpServletRequest, AwsHttpServletResponse> {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    // The Jersey application object
    private Application jaxRsApplication;

    private JerseyHandlerFilter jerseyFilter;
    // tracker for the first request
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
        return new JerseyLambdaContainerHandler<>(new AwsProxyHttpServletRequestReader(),
                                                  new AwsProxyHttpServletResponseWriter(),
                                                  new AwsProxySecurityContextWriter(),
                                                  new AwsProxyExceptionHandler(),
                                                  jaxRsApplication);
    }


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    /**
     * Private constructor for a LambdaContainer. Sets the application object, sets the ApplicationHandler,
     * and initializes the application using the <code>onStartup</code> method.
     * @param requestReader A request reader instance
     * @param responseWriter A response writer instance
     * @param securityContextWriter A security context writer object
     * @param exceptionHandler An exception handler
     * @param jaxRsApplication The JaxRs application
     */
    public JerseyLambdaContainerHandler(RequestReader<RequestType, AwsProxyHttpServletRequest> requestReader,
                                        ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                        SecurityContextWriter<RequestType> securityContextWriter,
                                        ExceptionHandler<ResponseType> exceptionHandler,
                                        Application jaxRsApplication) {

        super(requestReader, responseWriter, securityContextWriter, exceptionHandler);
        Timer.start("JERSEY_CONTAINER_CONSTRUCTOR");
        this.jaxRsApplication = jaxRsApplication;
        this.initialized = false;
        this.jerseyFilter = new JerseyHandlerFilter(this.jaxRsApplication);
        Timer.stop("JERSEY_CONTAINER_CONSTRUCTOR");
    }

    //-------------------------------------------------------------
    // Methods - Implementation
    //-------------------------------------------------------------

    @Override
    protected AwsHttpServletResponse getContainerResponse(AwsProxyHttpServletRequest request, CountDownLatch latch) {
        return new AwsHttpServletResponse(request, latch);
    }

    @Override
    protected void handleRequest(AwsProxyHttpServletRequest httpServletRequest, AwsHttpServletResponse httpServletResponse, Context lambdaContext)
            throws Exception {

        Timer.start("JERSEY_HANDLE_REQUEST");
        // this method of the AwsLambdaServletContainerHandler sets the request context
        super.handleRequest(httpServletRequest, httpServletResponse, lambdaContext);

        if (!initialized) {
            Timer.start("JERSEY_COLD_START_INIT");
            // call the onStartup event if set to give developers a chance to set filters in the context
            if (startupHandler != null) {
                startupHandler.onStartup(getServletContext());
            }

            // manually add the spark filter to the chain. This should the last one and match all uris
            FilterRegistration.Dynamic jerseyFilterReg = getServletContext().addFilter("JerseyFilter", jerseyFilter);
            jerseyFilterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

            initialized = true;
            Timer.stop("JERSEY_COLD_START_INIT");
        }

        httpServletRequest.setServletContext(getServletContext());

        doFilter(httpServletRequest, httpServletResponse, null);
        Timer.stop("JERSEY_HANDLE_REQUEST");
    }
}
