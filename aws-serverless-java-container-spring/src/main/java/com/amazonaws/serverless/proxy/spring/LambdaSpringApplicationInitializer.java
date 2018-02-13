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

import com.amazonaws.serverless.proxy.internal.testutils.Timer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
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
@SuppressFBWarnings("MTIA_SUSPECT_SERVLET_INSTANCE_FIELD") // we assume we are running in a "single threaded" environment - AWS Lambda
public class LambdaSpringApplicationInitializer extends HttpServlet implements WebApplicationInitializer {
    public static final String ERROR_NO_CONTEXT = "No application context or configuration classes provided";

    private static final String DEFAULT_SERVLET_NAME = "aws-servless-java-container";

    private static final long serialVersionUID = 42L;

    // all of these variables are declared as volatile for correctness, technically this class is an implementation of
    // HttpServlet and could live in a multi-threaded environment. Because the library runs inside AWS Lambda, we can
    // assume we will be in a single-threaded environment.

    // Configuration variables that can be passed in
    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    private transient volatile ConfigurableWebApplicationContext applicationContext;
    private volatile boolean refreshContext = true;
    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    private transient volatile List<ServletContextListener> contextListeners;
    private volatile ArrayList<String> springProfiles;

    // Dynamically instantiated properties
    private volatile DispatcherServlet dispatcherServlet;

    // The current response is used to release the latch when Spring emits the request handled event
    private transient volatile HttpServletResponse currentResponse;

    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    private transient Logger log = LoggerFactory.getLogger(LambdaSpringApplicationInitializer.class);

    /**
     * Creates a new instance of the WebApplicationInitializer
     * @param applicationContext A custom ConfigurableWebApplicationContext to be used
     */
    public LambdaSpringApplicationInitializer(ConfigurableWebApplicationContext applicationContext) {
        this.contextListeners = new ArrayList<>();
        this.applicationContext = applicationContext;
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

    public void setRefreshContext(boolean refreshContext) {
        this.refreshContext = refreshContext;
    }

    /**
     * Given a request and response objects, triggers the filters set in the servlet context and
     * @param request The incoming request
     * @param response The response object Spring should write to.
     * @throws ServletException When an error occurs during processing or of the request
     * @throws IOException When an error occurs while writing the response
     */
    public void dispatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        currentResponse = response;
        dispatcherServlet.service(request, response);
    }

    /**
     * Gets the initialized Spring dispatcher servlet instance.
     * @return The Spring dispatcher servlet
     */
    public DispatcherServlet getDispatcherServlet() {
        return dispatcherServlet;
    }

    public List<String> getSpringProfiles() {
        return Collections.unmodifiableList(springProfiles);
    }

    public void setSpringProfiles(List<String> springProfiles) {
        this.springProfiles = new ArrayList<>(springProfiles);
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        Timer.start("SPRING_INITIALIZER_ONSTARTUP");
        if (springProfiles != null) {
            applicationContext.getEnvironment().setActiveProfiles(springProfiles.toArray(new String[0]));
        }
        applicationContext.setServletContext(servletContext);


        DefaultDispatcherConfig dispatcherConfig = new DefaultDispatcherConfig(servletContext);
        applicationContext.setServletConfig(dispatcherConfig);

        // Configure the listener for the request handled events. All we do here is release the latch
        applicationContext.addApplicationListener(new ApplicationListener<ServletRequestHandledEvent>() {
            @Override
            public void onApplicationEvent(ServletRequestHandledEvent servletRequestHandledEvent) {
                try {
                    currentResponse.flushBuffer();
                } catch (IOException e) {
                    log.error("Could not flush response buffer", e);
                    throw new RuntimeException("Could not flush response buffer", e);
                }
            }
        });

        // Manage the lifecycle of the root application context
        this.addListener(new ContextLoaderListener(applicationContext));

        // Register and map the dispatcher servlet
        dispatcherServlet = new DispatcherServlet(applicationContext);

        if (refreshContext) {
            dispatcherServlet.refresh();
        }

        dispatcherServlet.onApplicationEvent(new ContextRefreshedEvent(applicationContext));
        dispatcherServlet.init(dispatcherConfig);

        notifyStartListeners(servletContext);
        Timer.stop("SPRING_INITIALIZER_ONSTARTUP");
    }

    private void notifyStartListeners(ServletContext context) {
        for (ServletContextListener listener : contextListeners) {
            listener.contextInitialized(new ServletContextEvent(context));
        }
    }


    ///////////////////////////////////////////////////////////////
    // HttpServlet implementation                                //
    // This is used to pass the initializer to the filter chain  //
    // to handle requests                                        //
    ///////////////////////////////////////////////////////////////

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        Timer.start("SPRING_INITIALIZER_SERVICE");
        Timer.start("SPRING_INITIALIZER_SERVICE_CAST");
        if (!(req instanceof HttpServletRequest)) {
            throw new ServletException("Cannot service request that is not instance of HttpServletRequest");
        }

        if (!(res instanceof HttpServletResponse)) {
            throw new ServletException("Cannot work with response that is not instance of HttpServletResponse");
        }
        Timer.stop("SPRING_INITIALIZER_SERVICE_CAST");
        dispatch((HttpServletRequest)req, (HttpServletResponse)res);
        Timer.stop("SPRING_INITIALIZER_SERVICE");
    }

    /**
     * Default configuration class for the DispatcherServlet. This just mocks the behaviour of a default
     * ServletConfig object with no init parameters
     */
    private static class DefaultDispatcherConfig implements ServletConfig {
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
