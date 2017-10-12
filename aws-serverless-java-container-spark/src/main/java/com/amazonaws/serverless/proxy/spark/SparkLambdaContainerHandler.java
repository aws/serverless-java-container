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
import com.amazonaws.serverless.proxy.internal.*;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.servlet.*;
import com.amazonaws.serverless.proxy.spark.embeddedserver.LambdaEmbeddedServer;
import com.amazonaws.serverless.proxy.spark.embeddedserver.LambdaEmbeddedServerFactory;

import com.amazonaws.services.lambda.runtime.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Service;
import spark.Spark;
import spark.embeddedserver.EmbeddedServerFactory;
import spark.embeddedserver.EmbeddedServers;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;

/**
 * Implementation of the <code>LambdaContainerHandler</code> object that supports the Spark framework: http://sparkjava.com/
 *
 * Because of the way this container is implemented, using reflection to change accessibility of methods in the Spark
 * framework and inserting itself as the default embedded container, it is important that you initialize the Handler
 * before declaring your spark routes.
 *
 * This implementation uses the default <code>AwsProxyHttpServletRequest</code> and Response implementations.
 *
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
 * @param <RequestType> The request object used by the <code>RequestReader</code> implementation passed to the constructor
 * @param <ResponseType> The response object produced by the <code>ResponseWriter</code> implementation in the constructor
 */
public class SparkLambdaContainerHandler<RequestType, ResponseType> extends AwsLambdaServletContainerHandler<RequestType, ResponseType, AwsProxyHttpServletRequest, AwsHttpServletResponse> {

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    private static final String LAMBDA_EMBEDDED_SERVER_CODE = "AWS_LAMBDA";


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private LambdaEmbeddedServer embeddedServer;
    private Logger log = LoggerFactory.getLogger(SparkLambdaContainerHandler.class);


    //-------------------------------------------------------------
    // Methods - Public - Static
    //-------------------------------------------------------------

    /**
     * Returns a new instance of an SparkLambdaContainerHandler initialized to work with <code>AwsProxyRequest</code>
     * and <code>AwsProxyResponse</code> objects.
     *
     * @return a new instance of <code>SparkLambdaContainerHandler</code>
     * @throws ContainerInitializationException Throws this exception if we fail to initialize the Spark container.
     *   This could be caused by the introspection used to insert the library as the default embedded container
     */
    public static SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler()
            throws ContainerInitializationException {
        return new SparkLambdaContainerHandler<>(new AwsProxyHttpServletRequestReader(),
                                                 new AwsProxyHttpServletResponseWriter(),
                                                 new AwsProxySecurityContextWriter(),
                                                 new AwsProxyExceptionHandler(),
                                                 new LambdaEmbeddedServerFactory());
    }


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public SparkLambdaContainerHandler(RequestReader<RequestType, AwsProxyHttpServletRequest> requestReader,
                                       ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                       SecurityContextWriter<RequestType> securityContextWriter,
                                       ExceptionHandler<ResponseType> exceptionHandler,
                                       EmbeddedServerFactory embeddedServerFactory)
            throws ContainerInitializationException {
        super(requestReader, responseWriter, securityContextWriter, exceptionHandler);

        EmbeddedServers.add(LAMBDA_EMBEDDED_SERVER_CODE, embeddedServerFactory);

        // TODO: This is pretty bad but we are not given access to the embeddedServerIdentifier property of the
        // Service object
        try {
            log.debug("Changing visibility of getInstance method and embeddedServerIdentifier properties");
            Method serviceInstanceMethod = Spark.class.getDeclaredMethod("getInstance");
            serviceInstanceMethod.setAccessible(true);
            Service sparkService = (Service) serviceInstanceMethod.invoke(null);
            Field serverIdentifierField = Service.class.getDeclaredField("embeddedServerIdentifier");
            serverIdentifierField.setAccessible(true);
            serverIdentifierField.set(sparkService, LAMBDA_EMBEDDED_SERVER_CODE);
        } catch (NoSuchFieldException e) {
            log.error("Could not fine embeddedServerIdentifier field in Service class", e);
            throw new ContainerInitializationException("Cannot find embeddedServerIdentifier field in Service class", e);
        } catch (NoSuchMethodException e) {
            log.error("Could not find getInstance method in Spark class", e);
            throw new ContainerInitializationException("Cannot find getInstance method in Spark class", e);
        } catch (IllegalAccessException e) {
            log.error("Could not access getInstance method in Spark class", e);
            throw new ContainerInitializationException("Cannot access getInstance method in Spark class", e);
        } catch (InvocationTargetException e) {
            log.error("Could not invoke getInstance method in Spark class", e);
            throw new ContainerInitializationException("Cannot invoke getInstance method in Spark class", e);
        }
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

        // this method of the AwsLambdaServletContainerHandler sets the request context
        super.handleRequest(httpServletRequest, httpServletResponse, lambdaContext);

        if (embeddedServer == null) {
            log.debug("First request, getting new server instance");
            embeddedServer = LambdaEmbeddedServerFactory.getServerInstance();

            // call the onStartup event if set to give developers a chance to set filters in the context
            if (startupHandler != null) {
                startupHandler.onStartup(getServletContext());
            }

            // manually add the spark filter to the chain. This should the last one and match all uris
            FilterRegistration.Dynamic sparkRegistration = getServletContext().addFilter("SparkFilter", embeddedServer.getSparkFilter());
            sparkRegistration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
        }

        doFilter(httpServletRequest, httpServletResponse, null);
    }
}
