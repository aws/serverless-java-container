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

import javax.servlet.Servlet;
import javax.servlet.ServletException;

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

        return getAwsProxyHandler(applicationContext);
    }

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `AwsProxyResponse` objects
     * @param applicationContext A custom ConfigurableWebApplicationContext to be used
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException
     */
    public static SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(ConfigurableWebApplicationContext applicationContext)
            throws ContainerInitializationException {
        SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> newHandler = new SpringLambdaContainerHandler<>(
                                                                                            AwsProxyRequest.class,
                                                                                            AwsProxyResponse.class,
                                                                                            new AwsProxyHttpServletRequestReader(),
                                                                                            new AwsProxyHttpServletResponseWriter(),
                                                                                            new AwsProxySecurityContextWriter(),
                                                                                            new AwsProxyExceptionHandler(),
                                                                                            applicationContext);
        newHandler.initialize();
        return newHandler;
    }

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `AwsProxyResponse` objects and sets the given profiles as active
     * @param applicationContext A custom ConfigurableWebApplicationContext to be used
     * @param profiles The spring profiles to activate
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException
     */
    public static SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(ConfigurableWebApplicationContext applicationContext, String... profiles)
            throws ContainerInitializationException {
        SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> newHandler = new SpringLambdaContainerHandler<>(
                AwsProxyRequest.class,
                AwsProxyResponse.class,
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                applicationContext);
        newHandler.activateSpringProfiles(profiles);
        newHandler.initialize();
        return newHandler;
    }

    /**
     * Creates a new container handler with the given reader and writer objects
     *
     * @param requestTypeClass The class for the incoming Lambda event
     * @param requestReader An implementation of `RequestReader`
     * @param responseWriter An implementation of `ResponseWriter`
     * @param securityContextWriter An implementation of `SecurityContextWriter`
     * @param exceptionHandler An implementation of `ExceptionHandler`
     * @throws ContainerInitializationException
     */
    public SpringLambdaContainerHandler(Class<RequestType> requestTypeClass,
                                        Class<ResponseType> responseTypeClass,
                                        RequestReader<RequestType, AwsProxyHttpServletRequest> requestReader,
                                        ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                        SecurityContextWriter<RequestType> securityContextWriter,
                                        ExceptionHandler<ResponseType> exceptionHandler,
                                        ConfigurableWebApplicationContext applicationContext)
            throws ContainerInitializationException {
        super(requestTypeClass, responseTypeClass, requestReader, responseWriter, securityContextWriter, exceptionHandler);
        Timer.start("SPRING_CONTAINER_HANDLER_CONSTRUCTOR");
        initialized = false;
        initializer = new LambdaSpringApplicationInitializer(applicationContext);
        Timer.stop("SPRING_CONTAINER_HANDLER_CONSTRUCTOR");
    }


    /**
     * Asks the custom web application initializer to refresh the Spring context.
     * @param refreshContext true if the context should be refreshed
     */
    public void setRefreshContext(boolean refreshContext) {
        this.initializer.setRefreshContext(refreshContext);
    }


    /**
     * Returns the custom web application initializer used by the object. The custom application initializer gives you access
     * to the dispatcher servlet.
     * @return The instance of the custom {@link org.springframework.web.WebApplicationInitializer}
     */
    public LambdaSpringApplicationInitializer getApplicationInitializer() {
        return initializer;
    }

    @Override
    protected AwsHttpServletResponse getContainerResponse(AwsProxyHttpServletRequest request, CountDownLatch latch) {
        return new AwsHttpServletResponse(request, latch);
    }


    /**
     * Activates the given Spring profiles in the application. This method will cause the context to be
     * refreshed. To use a single Spring profile, use the static method {@link SpringLambdaContainerHandler#getAwsProxyHandler(ConfigurableWebApplicationContext, String...)}
     * @param profiles A number of spring profiles
     * @throws ContainerInitializationException if the initializer is not set yet.
     */
    public void activateSpringProfiles(String... profiles) throws ContainerInitializationException {
        if (initializer == null) {
            throw new ContainerInitializationException(LambdaSpringApplicationInitializer.ERROR_NO_CONTEXT, null);
        }
        setServletContext(new AwsServletContext(this));
        initializer.setSpringProfiles(getServletContext(), profiles);
    }

    @Override
    protected void handleRequest(AwsProxyHttpServletRequest containerRequest, AwsHttpServletResponse containerResponse, Context lambdaContext) throws Exception {
        Timer.start("SPRING_HANDLE_REQUEST");
        if (initializer == null) {
            throw new ContainerInitializationException(LambdaSpringApplicationInitializer.ERROR_NO_CONTEXT, null);
        }

        // wire up the application context on the first invocation
        if (!initialized) {
            initialize();
        }

        containerRequest.setServletContext(getServletContext());

        // process filters
        doFilter(containerRequest, containerResponse, initializer);
        Timer.stop("SPRING_HANDLE_REQUEST");
    }


    @Override
    public void initialize()
            throws ContainerInitializationException {
        Timer.start("SPRING_COLD_START");

        try {
            initializer.onStartup(getServletContext());
        } catch (ServletException e) {
            throw new ContainerInitializationException("Could not initialize Spring", e);
        }

        initialized = true;
        Timer.stop("SPRING_COLD_START");
    }

    public Servlet getServlet() {
        return initializer.getDispatcherServlet();
    }
}
