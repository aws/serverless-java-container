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
import com.amazonaws.serverless.proxy.spring.embedded.ServerlessServletEmbeddedServerFactory;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.WebApplicationInitializer;

import javax.servlet.*;

import java.util.concurrent.CountDownLatch;

/**
 * SpringBoot 1.x implementation of the `LambdaContainerHandler` abstract class. This class uses the `LambdaSpringApplicationInitializer`
 * object behind the scenes to proxy requests. The default implementation leverages the `AwsProxyHttpServletRequest` and
 * `AwsHttpServletResponse` implemented in the `aws-serverless-java-container-core` package.
 *
 * Important: Make sure to add <code>LambdaFlushResponseListener</code> in your SpringBootServletInitializer subclass configure().
 *
 * @param <RequestType> The incoming event type
 * @param <ResponseType> The expected return type
 */
public class SpringBootLambdaContainerHandler<RequestType, ResponseType> extends AwsLambdaServletContainerHandler<RequestType, ResponseType, AwsProxyHttpServletRequest, AwsHttpServletResponse> {
    private final Class<? extends WebApplicationInitializer> springBootInitializer;
    private static final Logger log = LoggerFactory.getLogger(SpringBootLambdaContainerHandler.class);
    private String[] springProfiles = null;

    private static SpringBootLambdaContainerHandler instance;

    // State vars
    private boolean initialized;

    /**
     * We need to rely on the static instance of this for SpringBoot because we need it to access the ServletContext.
     * Normally, SpringBoot would initialize its own embedded container through the <code>SpringApplication.run()</code>
     * method. However, in our case we need to rely on the pre-initialized handler and need to fetch information from it
     * for our mock {@link com.amazonaws.serverless.proxy.spring.embedded.ServerlessServletEmbeddedServerFactory}.
     *
     * @return The initialized instance
     */
    public static SpringBootLambdaContainerHandler getInstance() {
        return instance;
    }

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `AwsProxyResponse` objects
     * @param springBootInitializer {@code SpringBootServletInitializer} class
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException If an error occurs while initializing the Spring framework
     */
    public static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(Class<? extends WebApplicationInitializer> springBootInitializer)
            throws ContainerInitializationException {
        SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> newHandler = new SpringBootLambdaContainerHandler<>(
                                                                                                AwsProxyRequest.class,
                                                                                                AwsProxyResponse.class,
                                                                                                new AwsProxyHttpServletRequestReader(),
                                                                                                new AwsProxyHttpServletResponseWriter(),
                                                                                                new AwsProxySecurityContextWriter(),
                                                                                                new AwsProxyExceptionHandler(),
                                                                                                springBootInitializer);
        newHandler.initialize();
        return newHandler;
    }

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `AwsProxyResponse` objects and the given Spring profiles
     * @param springBootInitializer {@code SpringBootServletInitializer} class
     * @param profiles A list of Spring profiles to activate
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException If an error occurs while initializing the Spring framework
     */
    public static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(Class<? extends WebApplicationInitializer> springBootInitializer, String... profiles)
            throws ContainerInitializationException {
        SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> newHandler = new SpringBootLambdaContainerHandler<>(
                AwsProxyRequest.class,
                AwsProxyResponse.class,
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                springBootInitializer);
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
     * @param springBootInitializer {@code SpringBootServletInitializer} class
     * @throws ContainerInitializationException If an error occurs while initializing the Spring framework
     */
    public SpringBootLambdaContainerHandler(Class<RequestType> requestTypeClass,
                                            Class<ResponseType> responseTypeClass,
                                            RequestReader<RequestType, AwsProxyHttpServletRequest> requestReader,
                                            ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                            SecurityContextWriter<RequestType> securityContextWriter,
                                            ExceptionHandler<ResponseType> exceptionHandler,
                                            Class<? extends WebApplicationInitializer> springBootInitializer)
            throws ContainerInitializationException {
        super(requestTypeClass, responseTypeClass, requestReader, responseWriter, securityContextWriter, exceptionHandler);
        Timer.start("SPRINGBOOT_CONTAINER_HANDLER_CONSTRUCTOR");
        initialized = false;
        this.springBootInitializer = springBootInitializer;
        SpringBootLambdaContainerHandler.setInstance(this);

        Timer.stop("SPRINGBOOT_CONTAINER_HANDLER_CONSTRUCTOR");
    }

    // this is not pretty. However, because SpringBoot wants to control all of the initialization
    // we need to access this handler as a singleton from the EmbeddedContainer to set the servlet
    // context and from the ServletConfigurationSupport implementation
    private static void setInstance(SpringBootLambdaContainerHandler h) {
        SpringBootLambdaContainerHandler.instance = h;
    }

    public void activateSpringProfiles(String... profiles) {
        springProfiles = profiles;
        // force a re-initialization
        initialized = false;
    }

    @Override
    protected AwsHttpServletResponse getContainerResponse(AwsProxyHttpServletRequest request, CountDownLatch latch) {
        return new AwsHttpServletResponse(request, latch);
    }

    @Override
    protected void handleRequest(AwsProxyHttpServletRequest containerRequest, AwsHttpServletResponse containerResponse, Context lambdaContext) throws Exception {
        // this method of the AwsLambdaServletContainerHandler sets the servlet context
        Timer.start("SPRINGBOOT_HANDLE_REQUEST");

        // wire up the application context on the first invocation
        if (!initialized) {
            initialize();
        }

        containerRequest.setServletContext(getServletContext());

        // process filters & invoke servlet
        Servlet reqServlet = ((AwsServletContext)getServletContext()).getServletForPath(containerRequest.getPathInfo());
        doFilter(containerRequest, containerResponse, reqServlet);
        Timer.stop("SPRINGBOOT_HANDLE_REQUEST");
    }


    @Override
    public void initialize()
            throws ContainerInitializationException {
        Timer.start("SPRINGBOOT_COLD_START");

        SpringApplication app = new SpringApplication(
                springBootInitializer,
                ServerlessServletEmbeddedServerFactory.class,
                SpringBootServletConfigurationSupport.class
        );
        if (springProfiles != null && springProfiles.length > 0) {
            ConfigurableEnvironment springEnv = new StandardEnvironment();
            springEnv.setActiveProfiles(springProfiles);
            app.setEnvironment(springEnv);
        }
        app.run();

        initialized = true;
        Timer.stop("SPRINGBOOT_COLD_START");
    }
}
