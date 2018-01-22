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
package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.ExceptionHandler;
import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.ResponseWriter;
import com.amazonaws.serverless.proxy.SecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.servlet.*;
import com.amazonaws.services.lambda.runtime.Context;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * Spring implementation of the `LambdaContainerHandler` abstract class. This class uses the `LambdaSpringApplicationInitializer`
 * object behind the scenes to proxy requests. The default implementation leverages the `AwsProxyHttpServletRequest` and
 * `AwsHttpServletResponse` implemented in the `aws-serverless-java-container-core` package.
 * @param <RequestType> The incoming event type
 * @param <ResponseType> The expected return type
 */
public class SpringLambdaContainerHandler<RequestType, ResponseType> extends AwsLambdaServletContainerHandler<RequestType, ResponseType, AwsProxyHttpServletRequest, AwsHttpServletResponse> {
    private LambdaSpringApplicationInitializer initializer;

    // State vars
    private boolean initialized;


    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `AwsProxyResponse` objects
     * @param config A set of classes annotated with the Spring @Configuration annotation
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException
     */
    public static SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(Class... config) throws ContainerInitializationException {
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.register(config);

        return new SpringLambdaContainerHandler<>(
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                applicationContext
        );
    }

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `AwsProxyResponse` objects
     * @param applicationContext A custom ConfigurableWebApplicationContext to be used
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException
     */
    public static SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(ConfigurableWebApplicationContext applicationContext)
            throws ContainerInitializationException {
        return new SpringLambdaContainerHandler<>(
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                applicationContext
        );
    }

    /**
     * Creates a new container handler with the given reader and writer objects
     *
     * @param requestReader An implementation of `RequestReader`
     * @param responseWriter An implementation of `ResponseWriter`
     * @param securityContextWriter An implementation of `SecurityContextWriter`
     * @param exceptionHandler An implementation of `ExceptionHandler`
     * @throws ContainerInitializationException
     */
    public SpringLambdaContainerHandler(RequestReader<RequestType, AwsProxyHttpServletRequest> requestReader,
                                        ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                        SecurityContextWriter<RequestType> securityContextWriter,
                                        ExceptionHandler<ResponseType> exceptionHandler,
                                        ConfigurableWebApplicationContext applicationContext)
            throws ContainerInitializationException {
        super(requestReader, responseWriter, securityContextWriter, exceptionHandler);
        Timer.start("SPRING_CONTAINER_HANDLER_CONSTRUCTOR");
        initializer = new LambdaSpringApplicationInitializer(applicationContext);
        Timer.stop("SPRING_CONTAINER_HANDLER_CONSTRUCTOR");
    }

    public void setRefreshContext(boolean refreshContext) {
        this.initializer.setRefreshContext(refreshContext);
    }

    @Override
    protected AwsHttpServletResponse getContainerResponse(AwsProxyHttpServletRequest request, CountDownLatch latch) {
        return new AwsHttpServletResponse(request, latch);
    }

    public void activateSpringProfiles(String... profiles) throws ContainerInitializationException {
        if (initializer == null) {
            throw new ContainerInitializationException(LambdaSpringApplicationInitializer.ERROR_NO_CONTEXT, null);
        }

        initializer.setSpringProfiles(Arrays.asList(profiles));
    }

    @Override
    protected void handleRequest(AwsProxyHttpServletRequest containerRequest, AwsHttpServletResponse containerResponse, Context lambdaContext) throws Exception {
        Timer.start("SPRING_HANDLE_REQUEST");
        if (initializer == null) {
            throw new ContainerInitializationException(LambdaSpringApplicationInitializer.ERROR_NO_CONTEXT, null);
        }

        // this method of the AwsLambdaServletContainerHandler sets the request context
        super.handleRequest(containerRequest, containerResponse, lambdaContext);

        // wire up the application context on the first invocation
        if (!initialized) {
            Timer.start("SPRING_COLD_START");

            initializer.onStartup(getServletContext());

            // call the onStartup event if set to give developers a chance to set filters in the context
            if (startupHandler != null) {
                startupHandler.onStartup(getServletContext());
            }

            initialized = true;
            Timer.stop("SPRING_COLD_START");
        }

        containerRequest.setServletContext(getServletContext());

        // process filters
        doFilter(containerRequest, containerResponse, initializer);
        Timer.stop("SPRING_HANDLE_REQUEST");
    }
}
