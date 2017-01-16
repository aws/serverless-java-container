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
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Custom implementation of Spring's `WebApplicationInitializer`. Uses internal variables to keep application state
 * and creates a DispatcherServlet to handle incoming events. When the first event arrives it extracts the ServletContext
 * and starts the Spring application. This class assumes that the implementation of `HttpServletRequest` that is passed
 * in correctly implements the `getServletContext` method.
 *
 * State is kept using the `initialized` boolean variable. Each time a new event is received, the app sets the
 * `currentResponse` private property to the value of the new `HttpServletResponse` object. This is used to intercept
 * Spring notifications for the `ServletRequestHandledEvent` and call the flush method to release the latch.
 */
public class LambdaSpringApplicationInitializer implements WebApplicationInitializer {
    public static final String ERROR_NO_CONTEXT = "No application context or configuration classes provided";

    private static final String DEFAULT_SERVLET_NAME = "aws-servless-java-container";

    // Configuration variables that can be passed in
    private List<Class> configurationClasses;
    private List<ServletContextListener> contextListeners;

    // Dynamically instantiated properties
    private WebApplicationContext applicationContext;
    private ServletConfig dispatcherConfig;
    private DispatcherServlet dispatcherServlet;

    // The current response is used to release the latch when Spring emits the request handled event
    private HttpServletResponse currentResponse;

    /**
     * Starts the application initializer with a set of annotated configuration classes. At the first request, this will
     * create a new instance of AnnotationWebApplicationContext using the array of configuration classes.
     *
     * @param configuration A set of configuration classes
     */
    public LambdaSpringApplicationInitializer(Class... configuration) {
        configurationClasses = new ArrayList<>(Arrays.asList(configuration));
        contextListeners = new ArrayList<>();
    }

    /**
     * Creates a new instance of LambdaSpringApplicationInitializer with an instance of applicationContext. When created
     * this way, annotated configuration classes have no effect and the library will only use the application context.
     *
     * @param applicationContext An initialized implementation of WebApplicationContext
     */
    public LambdaSpringApplicationInitializer(WebApplicationContext applicationContext) {
        configurationClasses = new ArrayList<>();
        this.applicationContext = applicationContext;
        contextListeners = new ArrayList<>();
    }

    /**
     * Adds a new listener for the servlet context events. At the moment the library only emits events when the application
     * is initialized. Because we don't have container lifecycle notifications from Lambda the `contextDestroyed`
     * method is never called
     * @param listener An implementation of `ServletContextListener`
     */
    public void addListener(ServletContextListener listener) {
        contextListeners.add(listener);
    }

    public void dispatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        currentResponse = response;
        dispatcherServlet.service(request, response);
    }

    /**
     * Returns the application context initialized in the library
     * @return
     */
    public WebApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        if (applicationContext == null) {
            if (configurationClasses.size() > 0) {
                applicationContext = initializeAnnotationApplicationContext(servletContext);
            } else {
                throw new ServletException(
                        new ContainerInitializationException(ERROR_NO_CONTEXT, null)
                );
            }
        }
        // Manage the lifecycle of the root application context
        this.addListener(new ContextLoaderListener(applicationContext));

        // Register and map the dispatcher servlet
        dispatcherServlet = new DispatcherServlet(applicationContext);
        dispatcherServlet.refresh();
        dispatcherServlet.onApplicationEvent(new ContextRefreshedEvent(applicationContext));
        dispatcherServlet.init(dispatcherConfig);

        notifyStartListeners(servletContext);
    }

    /**
     * Initializes a new AnnotationConfigWebApplicationContext using the list of annotated configuration classes
     * passed to the constructor
     *
     * @param servletContext The context from the servlet container
     * @return A new instance of WebApplicationContext using the config classes passed to the constructor
     */
    private AnnotationConfigWebApplicationContext initializeAnnotationApplicationContext(ServletContext servletContext) {
        // Create the 'root' Spring application context
        AnnotationConfigWebApplicationContext newApplicationContext = new AnnotationConfigWebApplicationContext();
        for (Class config : configurationClasses) {
            newApplicationContext.register(config);
        }
        newApplicationContext.setServletContext(servletContext);

        dispatcherConfig = new DefaultDispatcherConfig(servletContext);
        newApplicationContext.setServletConfig(dispatcherConfig);

        // Configure the listener for the request handled events. All we do here is release the latch
        newApplicationContext.addApplicationListener(new ApplicationListener<ServletRequestHandledEvent>() {
            @Override
            public void onApplicationEvent(ServletRequestHandledEvent servletRequestHandledEvent) {
                try {
                    currentResponse.flushBuffer();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Could not flush response buffer", e);
                }
            }
        });

        return newApplicationContext;
    }

    private void notifyStartListeners(ServletContext context) {
        for (ServletContextListener listener : contextListeners) {
            listener.contextInitialized(new ServletContextEvent(context));
        }
    }

    /**
     * Default configuration class for the DispatcherServlet. This just mocks the behaviour of a default
     * ServletConfig object with no init parameters
     */
    private class DefaultDispatcherConfig implements ServletConfig {
        private ServletContext servletContext;

        DefaultDispatcherConfig(ServletContext context) {
            servletContext = context;
        }

        @Override
        public String getServletName() {
            return DEFAULT_SERVLET_NAME;
        }

        @Override
        public ServletContext getServletContext() {
            return servletContext;
        }

        @Override
        public String getInitParameter(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.emptyEnumeration();
        }
    }
}
