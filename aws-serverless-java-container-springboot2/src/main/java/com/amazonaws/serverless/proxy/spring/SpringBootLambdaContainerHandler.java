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
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.*;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.embedded.ServerlessReactiveServletEmbeddedServerFactory;
import com.amazonaws.serverless.proxy.spring.embedded.ServerlessServletEmbeddedServerFactory;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.Servlet;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
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
public class SpringBootLambdaContainerHandler<RequestType, ResponseType> extends AwsLambdaServletContainerHandler<RequestType, ResponseType, HttpServletRequest, AwsHttpServletResponse> {
    private static final String DISPATCHER_SERVLET_REGISTRATION_NAME = "dispatcherServlet";

    private final Class<?> springBootInitializer;
    private static final Logger log = LoggerFactory.getLogger(SpringBootLambdaContainerHandler.class);
    private String[] springProfiles = null;
    private WebApplicationType springWebApplicationType;
    private ConfigurableApplicationContext applicationContext;

    private static SpringBootLambdaContainerHandler instance;

    // State vars
    private boolean initialized;

    /**
     * We need to rely on the static instance of this for SpringBoot because we need it to access the ServletContext.
     * Normally, SpringBoot would initialize its own embedded container through the <code>SpringApplication.run()</code>
     * method. However, in our case we need to rely on the pre-initialized handler and need to fetch information from it
     * for our mock {@link ServerlessReactiveServletEmbeddedServerFactory}.
     *
     * @return The initialized instance
     */
    public static SpringBootLambdaContainerHandler getInstance() {
        return instance;
    }

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `AwsProxyResponse` objects and the given Spring profiles
     * @param springBootInitializer {@code SpringBootServletInitializer} class
     * @param profiles A list of Spring profiles to activate
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException If an error occurs while initializing the Spring framework
     */
    public static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(Class<?> springBootInitializer, String... profiles)
            throws ContainerInitializationException {
        return new SpringBootProxyHandlerBuilder<AwsProxyRequest>()
                .defaultProxy()
                .initializationWrapper(new InitializationWrapper())
                .springBootApplication(springBootInitializer)
                .profiles(profiles)
                .buildAndInitialize();
    }

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `HttpApiV2ProxyRequest` objects and the given Spring profiles
     * @param springBootInitializer {@code SpringBootServletInitializer} class
     * @param profiles A list of Spring profiles to activate
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException If an error occurs while initializing the Spring framework
     */
    public static SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> getHttpApiV2ProxyHandler(Class<?> springBootInitializer, String... profiles)
            throws ContainerInitializationException {
        return new SpringBootProxyHandlerBuilder<HttpApiV2ProxyRequest>()
                .defaultHttpApiV2Proxy()
                .initializationWrapper(new InitializationWrapper())
                .springBootApplication(springBootInitializer)
                .profiles(profiles)
                .buildAndInitialize();
    }

    /**
     * Creates a new container handler with the given reader and writer objects
     *
     * @param requestTypeClass The class for the incoming Lambda event
     * @param responseTypeClass The class for the Lambda function output
     * @param requestReader An implementation of `RequestReader`
     * @param responseWriter An implementation of `ResponseWriter`
     * @param securityContextWriter An implementation of `SecurityContextWriter`
     * @param exceptionHandler An implementation of `ExceptionHandler`
     * @param springBootInitializer {@code SpringBootServletInitializer} class
     * @param init The initialization Wrapper that will be used to start Spring Boot
     * @param applicationType The Spring Web Application Type
     */
    public SpringBootLambdaContainerHandler(Class<RequestType> requestTypeClass,
                                            Class<ResponseType> responseTypeClass,
                                            RequestReader<RequestType, HttpServletRequest> requestReader,
                                            ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                            SecurityContextWriter<RequestType> securityContextWriter,
                                            ExceptionHandler<ResponseType> exceptionHandler,
                                            Class<?> springBootInitializer,
                                            InitializationWrapper init,
                                            WebApplicationType applicationType) {
        super(requestTypeClass, responseTypeClass, requestReader, responseWriter, securityContextWriter, exceptionHandler);
        Timer.start("SPRINGBOOT2_CONTAINER_HANDLER_CONSTRUCTOR");
        initialized = false;
        this.springBootInitializer = springBootInitializer;
        springWebApplicationType = applicationType;
        setInitializationWrapper(init);
        SpringBootLambdaContainerHandler.setInstance(this);

        Timer.stop("SPRINGBOOT2_CONTAINER_HANDLER_CONSTRUCTOR");
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
    protected AwsHttpServletResponse getContainerResponse(HttpServletRequest request, CountDownLatch latch) {
        return new AwsHttpServletResponse(request, latch);
    }

    @Override
    protected void handleRequest(HttpServletRequest containerRequest, AwsHttpServletResponse containerResponse, Context lambdaContext) throws Exception {
        // this method of the AwsLambdaServletContainerHandler sets the servlet context
        Timer.start("SPRINGBOOT2_HANDLE_REQUEST");

        // wire up the application context on the first invocation
        if (!initialized) {
            initialize();
        }

        // process filters & invoke servlet
        Servlet reqServlet = ((AwsServletContext)getServletContext()).getServletForPath(containerRequest.getPathInfo());
        if (AwsHttpServletRequest.class.isAssignableFrom(containerRequest.getClass())) {
            ((AwsHttpServletRequest)containerRequest).setServletContext(getServletContext());
            ((AwsHttpServletRequest)containerRequest).setResponse(containerResponse);
        }
        doFilter(containerRequest, containerResponse, reqServlet);
        Timer.stop("SPRINGBOOT2_HANDLE_REQUEST");
    }


    @Override
    public void initialize()
            throws ContainerInitializationException {
        Timer.start("SPRINGBOOT2_COLD_START");

        SpringApplicationBuilder builder = new SpringApplicationBuilder(getEmbeddedContainerClasses())
                .web(springWebApplicationType); // .REACTIVE, .SERVLET
        if (springProfiles != null) {
            builder.profiles(springProfiles);
        }
        applicationContext = builder.run();
        if (springWebApplicationType == WebApplicationType.SERVLET) {
            ((AnnotationConfigServletWebServerApplicationContext)applicationContext).setServletContext(getServletContext());
            AwsServletRegistration reg = (AwsServletRegistration)getServletContext().getServletRegistration(DISPATCHER_SERVLET_REGISTRATION_NAME);
            if (reg != null) {
                reg.setLoadOnStartup(1);
            }
        }
        super.initialize();
        initialized = true;
        Timer.stop("SPRINGBOOT2_COLD_START");
    }

    private Class<?>[] getEmbeddedContainerClasses() {
        Class<?>[] classes = new Class[2];
        if (springWebApplicationType == WebApplicationType.REACTIVE) {
            try {
                // if HandlerAdapter is available we assume they are using WebFlux. Otherwise plain servlet.
                this.getClass().getClassLoader().loadClass("org.springframework.web.reactive.HandlerAdapter");
                log.debug("Found WebFlux HandlerAdapter on classpath, using reactive server factory");
                classes[0] = ServerlessReactiveServletEmbeddedServerFactory.class;
            } catch (ClassNotFoundException e) {
                springWebApplicationType = WebApplicationType.SERVLET;
                classes[0] = ServerlessServletEmbeddedServerFactory.class;
            }
        } else {
            classes[0] = ServerlessServletEmbeddedServerFactory.class;
        }

        classes[1] = springBootInitializer;
        return classes;
    }
}
