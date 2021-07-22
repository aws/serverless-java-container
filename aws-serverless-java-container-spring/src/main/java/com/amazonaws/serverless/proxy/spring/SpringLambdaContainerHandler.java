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
import com.amazonaws.serverless.proxy.*;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.servlet.*;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.Servlet;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;

import java.util.concurrent.CountDownLatch;

/**
 * Spring implementation of the `LambdaContainerHandler` abstract class. This class uses the `LambdaSpringApplicationInitializer`
 * object behind the scenes to proxy requests. The default implementation leverages the `AwsProxyHttpServletRequest` and
 * `AwsHttpServletResponse` implemented in the `aws-serverless-java-container-core` package.
 * @param <RequestType> The incoming event type
 * @param <ResponseType> The expected return type
 */
public class SpringLambdaContainerHandler<RequestType, ResponseType> extends AwsLambdaServletContainerHandler<RequestType, ResponseType, HttpServletRequest, AwsHttpServletResponse> {
    protected final ConfigurableWebApplicationContext appContext;
    private String[] profiles;

    // State vars
    private boolean refreshContext = false;

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `AwsProxyResponse` objects
     * @param config A set of classes annotated with the Spring @Configuration annotation
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException When the Spring framework fails to start.
     */
    public static SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(Class<?>... config) throws ContainerInitializationException {
        return new SpringProxyHandlerBuilder<AwsProxyRequest>()
                .defaultProxy()
                .initializationWrapper(new InitializationWrapper())
                .configurationClasses(config)
                .buildAndInitialize();
    }

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `AwsProxyResponse` objects and sets the given profiles as active
     * @param applicationContext A custom ConfigurableWebApplicationContext to be used
     * @param profiles The spring profiles to activate
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException When the Spring framework fails to start.
     */
    public static SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(ConfigurableWebApplicationContext applicationContext, String... profiles)
            throws ContainerInitializationException {
        return new SpringProxyHandlerBuilder<AwsProxyRequest>()
                .defaultProxy()
                .initializationWrapper(new InitializationWrapper())
                .springApplicationContext(applicationContext)
                .profiles(profiles)
                .buildAndInitialize();
    }

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `HttpApiV2ProxyRequest` and `AwsProxyResponse` objects
     * @param config A set of classes annotated with the Spring @Configuration annotation
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException When the Spring framework fails to start.
     */
    public static SpringLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> getHttpApiV2ProxyHandler(Class<?>... config) throws ContainerInitializationException {
        return new SpringProxyHandlerBuilder<HttpApiV2ProxyRequest>()
                .defaultHttpApiV2Proxy()
                .initializationWrapper(new InitializationWrapper())
                .configurationClasses(config)
                .buildAndInitialize();
    }

    /**
     * Creates a new container handler with the given reader and writer objects
     *
     * @param requestTypeClass The class for the incoming Lambda event
     * @param requestReader An implementation of `RequestReader`
     * @param responseWriter An implementation of `ResponseWriter`
     * @param securityContextWriter An implementation of `SecurityContextWriter`
     * @param exceptionHandler An implementation of `ExceptionHandler`
     */
    public SpringLambdaContainerHandler(Class<RequestType> requestTypeClass,
                                        Class<ResponseType> responseTypeClass,
                                        RequestReader<RequestType, HttpServletRequest> requestReader,
                                        ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                        SecurityContextWriter<RequestType> securityContextWriter,
                                        ExceptionHandler<ResponseType> exceptionHandler,
                                        ConfigurableWebApplicationContext applicationContext,
                                        InitializationWrapper init) {
        super(requestTypeClass, responseTypeClass, requestReader, responseWriter, securityContextWriter, exceptionHandler);
        Timer.start("SPRING_CONTAINER_HANDLER_CONSTRUCTOR");
        appContext = applicationContext;
        setInitializationWrapper(init);
        Timer.stop("SPRING_CONTAINER_HANDLER_CONSTRUCTOR");
    }


    /**
     * Asks the custom web application initializer to refresh the Spring context.
     * @param refresh true if the context should be refreshed
     */
    public void setRefreshContext(boolean refresh) {
        //this.initializer.setRefreshContext(refreshContext);
        refreshContext = refresh;
    }


    @Override
    protected AwsHttpServletResponse getContainerResponse(HttpServletRequest request, CountDownLatch latch) {
        return new AwsHttpServletResponse(request, latch);
    }


    /**
     * Activates the given Spring profiles in the application. This method will cause the context to be
     * refreshed. To use a single Spring profile, use the static method {@link SpringLambdaContainerHandler#getAwsProxyHandler(ConfigurableWebApplicationContext, String...)}
     * @param p A number of spring profiles
     * @throws ContainerInitializationException if the initializer is not set yet.
     */
    public void activateSpringProfiles(String... p) throws ContainerInitializationException {
        profiles = p;
        setServletContext(new AwsServletContext(this));
        appContext.registerShutdownHook();
        appContext.close();
        initialize();
    }

    @Override
    protected void handleRequest(HttpServletRequest containerRequest, AwsHttpServletResponse containerResponse, Context lambdaContext) throws Exception {
        Timer.start("SPRING_HANDLE_REQUEST");

        if (refreshContext) {
            appContext.refresh();
            refreshContext = false;
        }

        if (AwsHttpServletRequest.class.isAssignableFrom(containerRequest.getClass())) {
            ((AwsHttpServletRequest)containerRequest).setServletContext(getServletContext());
            ((AwsHttpServletRequest)containerRequest).setResponse(containerResponse);
        }

        // process filters
        Servlet reqServlet = ((AwsServletContext)getServletContext()).getServletForPath(containerRequest.getPathInfo());
        doFilter(containerRequest, containerResponse, reqServlet);
        Timer.stop("SPRING_HANDLE_REQUEST");
    }


    @Override
    public void initialize()
            throws ContainerInitializationException {
        Timer.start("SPRING_COLD_START");
        if (profiles != null) {
            appContext.getEnvironment().setActiveProfiles(profiles);
        }
        appContext.setServletContext(getServletContext());
        registerServlets();
        // call initialize on AwsLambdaServletContainerHandler to initialize servlets that are set to load on startup
        super.initialize();
        Timer.stop("SPRING_COLD_START");
    }

    /**
     * Overriding this method allows to customize the standard Spring DispatcherServlet
     * or to register additional servlets
     */
    protected void registerServlets() {
        DispatcherServlet dispatcher = new DispatcherServlet(appContext);
        ServletRegistration.Dynamic reg = getServletContext().addServlet("dispatcherServlet", dispatcher);
        reg.addMapping("/");
        reg.setLoadOnStartup(1);
    }
}
