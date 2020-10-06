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
package com.amazonaws.serverless.proxy.spark;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.*;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.servlet.*;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spark.embeddedserver.LambdaEmbeddedServer;
import com.amazonaws.serverless.proxy.spark.embeddedserver.LambdaEmbeddedServerFactory;

import com.amazonaws.services.lambda.runtime.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Service;
import spark.Spark;
import spark.embeddedserver.EmbeddedServers;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;


/**
 * Implementation of the <code>LambdaContainerHandler</code> object that supports the Spark framework: http://sparkjava.com/
 * <p>
 * Because of the way this container is implemented, using reflection to change accessibility of methods in the Spark
 * framework and inserting itself as the default embedded container, it is important that you initialize the Handler
 * before declaring your spark routes.
 * <p>
 * This implementation uses the default <code>AwsProxyHttpServletRequest</code> and Response implementations.
 * <p>
 * <pre>
 * {@code
 *     // always initialize the handler first
 *     SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler =
 *             SparkLambdaContainerHandler.getAwsProxyHandler();
 *
 *     get("/hello", (req, res) -> {
 *         res.status(200);
 *         res.body("Hello World");
 *     });
 * }
 * </pre>
 *
 * @param <RequestType> The request object used by the <code>RequestReader</code> implementation passed to the constructor
 * @param <ResponseType> The response object produced by the <code>ResponseWriter</code> implementation in the constructor
 */
public class SparkLambdaContainerHandler<RequestType, ResponseType>
        extends AwsLambdaServletContainerHandler<RequestType, ResponseType, HttpServletRequest, AwsHttpServletResponse> {

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    private static final String LAMBDA_EMBEDDED_SERVER_CODE = "AWS_LAMBDA";

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private LambdaEmbeddedServer embeddedServer;
    private LambdaEmbeddedServerFactory lambdaServerFactory;
    private Logger log = LoggerFactory.getLogger(SparkLambdaContainerHandler.class);

    //-------------------------------------------------------------
    // Methods - Public - Static
    //-------------------------------------------------------------


    /**
     * Returns a new instance of an SparkLambdaContainerHandler initialized to work with <code>AwsProxyRequest</code>
     * and <code>AwsProxyResponse</code> objects.
     *
     * @return a new instance of <code>SparkLambdaContainerHandler</code>
     *
     * @throws ContainerInitializationException Throws this exception if we fail to initialize the Spark container.
     * This could be caused by the introspection used to insert the library as the default embedded container
     */
    public static SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler()
            throws ContainerInitializationException {
        SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> newHandler = new SparkLambdaContainerHandler<>(AwsProxyRequest.class,
                                                                                         AwsProxyResponse.class,
                                                                                         new AwsProxyHttpServletRequestReader(),
                                                                                         new AwsProxyHttpServletResponseWriter(),
                                                                                         new AwsProxySecurityContextWriter(),
                                                                                         new AwsProxyExceptionHandler(),
                                                                                         new LambdaEmbeddedServerFactory());

        // For Spark we cannot call initialize here. It needs to be called manually after the routes are set
        //newHandler.initialize();

        return newHandler;
    }

    /**
     * Returns a new instance of an SparkLambdaContainerHandler initialized to work with <code>HttpApiV2ProxyRequest</code>
     * and <code>AwsProxyResponse</code> objects.
     *
     * @return a new instance of <code>SparkLambdaContainerHandler</code>
     *
     * @throws ContainerInitializationException Throws this exception if we fail to initialize the Spark container.
     * This could be caused by the introspection used to insert the library as the default embedded container
     */
    public static SparkLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> getHttpApiV2ProxyHandler()
            throws ContainerInitializationException {
        SparkLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> newHandler = new SparkLambdaContainerHandler<>(HttpApiV2ProxyRequest.class,
                AwsProxyResponse.class,
                new AwsHttpApiV2HttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(true),
                new AwsHttpApiV2SecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                new LambdaEmbeddedServerFactory());

        // For Spark we cannot call initialize here. It needs to be called manually after the routes are set
        //newHandler.initialize();

        return newHandler;
    }

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------


    public SparkLambdaContainerHandler(Class<RequestType> requestTypeClass,
                                       Class<ResponseType> responseTypeClass,
                                       RequestReader<RequestType, HttpServletRequest> requestReader,
                                       ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                       SecurityContextWriter<RequestType> securityContextWriter,
                                       ExceptionHandler<ResponseType> exceptionHandler,
                                       LambdaEmbeddedServerFactory embeddedServerFactory)
            throws ContainerInitializationException {
        super(requestTypeClass, responseTypeClass, requestReader, responseWriter, securityContextWriter, exceptionHandler);
        Timer.start("SPARK_CONTAINER_HANDLER_CONSTRUCTOR");

        EmbeddedServers.add(LAMBDA_EMBEDDED_SERVER_CODE, embeddedServerFactory);
        this.lambdaServerFactory = embeddedServerFactory;

        // TODO: This is pretty bad but we are not given access to the embeddedServerIdentifier property of the
        // Service object
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                log.debug("Changing visibility of getInstance method and embeddedServerIdentifier properties");
                Method serviceInstanceMethod = Spark.class.getDeclaredMethod("getInstance");
                serviceInstanceMethod.setAccessible(true);
                Service sparkService = (Service) serviceInstanceMethod.invoke(null);
                Field serverIdentifierField = Service.class.getDeclaredField("embeddedServerIdentifier");
                serverIdentifierField.setAccessible(true);
                serverIdentifierField.set(sparkService, LAMBDA_EMBEDDED_SERVER_CODE);
                return null;
            });
        } catch (PrivilegedActionException e) {
            if (e.getException() instanceof NoSuchFieldException) {
                log.error("Could not fine embeddedServerIdentifier field in Service class", e.getException());
            } else if (e.getException() instanceof NoSuchMethodException) {
                log.error("Could not find getInstance method in Spark class", e.getException());
            } else if (e.getException() instanceof IllegalAccessException) {
                log.error("Could not access getInstance method in Spark class", e.getException());
            } else if (e.getException() instanceof InvocationTargetException) {
                log.error("Could not invoke getInstance method in Spark class", e.getException());
            } else {
                log.error("Unknown exception while modifying Spark class", e.getException());
            }
            Timer.stop("SPARK_CONTAINER_HANDLER_CONSTRUCTOR");
            throw new ContainerInitializationException("Could not initialize Spark server", e.getException());
        }
        Timer.stop("SPARK_CONTAINER_HANDLER_CONSTRUCTOR");
    }

    //-------------------------------------------------------------
    // Methods - Implementation
    //-------------------------------------------------------------


    @Override
    protected AwsHttpServletResponse getContainerResponse(HttpServletRequest request, CountDownLatch latch) {
        return new AwsHttpServletResponse(request, latch);
    }


    @Override
    protected void handleRequest(HttpServletRequest httpServletRequest, AwsHttpServletResponse httpServletResponse, Context lambdaContext)
            throws Exception {
        Timer.start("SPARK_HANDLE_REQUEST");

        if (embeddedServer == null) {
            initialize();
        }

        if (AwsHttpServletRequest.class.isAssignableFrom(httpServletRequest.getClass())) {
            ((AwsHttpServletRequest)httpServletRequest).setServletContext(getServletContext());
        }

        doFilter(httpServletRequest, httpServletResponse, null);
        Timer.stop("SPARK_HANDLE_REQUEST");
    }


    @Override
    public void initialize()
            throws ContainerInitializationException {
        Timer.start("SPARK_COLD_START");
        log.debug("First request, getting new server instance");

        // trying to call init in case the embedded server had not been initialized.
        Spark.init();

        // adding this call to make sure that the framework is fully initialized. This should address a race
        // condition and solve GitHub issue #71.
        Spark.awaitInitialization();

        embeddedServer = lambdaServerFactory.getServerInstance();

        // manually add the spark filter to the chain. This should the last one and match all uris
        FilterRegistration.Dynamic sparkRegistration = getServletContext().addFilter("SparkFilter", embeddedServer.getSparkFilter());
        sparkRegistration.addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.INCLUDE, DispatcherType.FORWARD),
                true, "/*");
        Timer.stop("SPARK_COLD_START");
    }

    public Servlet getServlet() {
        return null;
    }
}
