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
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.servlet.*;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Spring implementation of the `LambdaContainerHandler` abstract class. This class uses the `LambdaSpringApplicationInitializer`
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

    // State vars
    private boolean initialized;

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `AwsProxyResponse` objects
     * @param springBootInitializer {@code SpringBootServletInitializer} class
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException If an error occurs while initializing the Spring framework
     */
    public static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(Class<? extends WebApplicationInitializer> springBootInitializer)
            throws ContainerInitializationException {
        return new SpringBootLambdaContainerHandler<>(
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                springBootInitializer
        );
    }

    /**
     * Creates a new container handler with the given reader and writer objects
     *
     * @param requestReader An implementation of `RequestReader`
     * @param responseWriter An implementation of `ResponseWriter`
     * @param securityContextWriter An implementation of `SecurityContextWriter`
     * @param exceptionHandler An implementation of `ExceptionHandler`
     * @param springBootInitializer {@code SpringBootServletInitializer} class
     * @throws ContainerInitializationException If an error occurs while initializing the Spring framework
     */
    public SpringBootLambdaContainerHandler(RequestReader<RequestType, AwsProxyHttpServletRequest> requestReader,
                                            ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                            SecurityContextWriter<RequestType> securityContextWriter,
                                            ExceptionHandler<ResponseType> exceptionHandler,
                                            Class<? extends WebApplicationInitializer> springBootInitializer)
            throws ContainerInitializationException {
        super(requestReader, responseWriter, securityContextWriter, exceptionHandler);
        this.springBootInitializer = springBootInitializer;
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
        if (getServletContext() == null) {
            setServletContext(new SpringBootAwsServletContext());
        }

        // wire up the application context on the first invocation
        if (!initialized) {
            if (springProfiles != null && springProfiles.length > 0) {
                System.setProperty("spring.profiles.active", String.join(",", springProfiles));
            }
            SpringServletContainerInitializer springServletContainerInitializer = new SpringServletContainerInitializer();
            LinkedHashSet<Class<?>> webAppInitializers = new LinkedHashSet<>();
            webAppInitializers.add(springBootInitializer);
            springServletContainerInitializer.onStartup(webAppInitializers, getServletContext());

            if (springProfiles != null && springProfiles.length > 0) {
                ConfigurableEnvironment springEnv = new StandardEnvironment();
                springEnv.setActiveProfiles(springProfiles);
                
            }

            initialized = true;
        }

        containerRequest.setServletContext(getServletContext());

        WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());

        DispatcherServlet dispatcherServlet = applicationContext.getBean("dispatcherServlet", DispatcherServlet.class);
        // process filters & invoke servlet
        doFilter(containerRequest, containerResponse, dispatcherServlet);
    }

    private class SpringBootAwsServletContext extends AwsServletContext {
        public SpringBootAwsServletContext() {
            super(SpringBootLambdaContainerHandler.this);
        }

        @Override
        public ServletRegistration.Dynamic addServlet(String s, String s1) {
            throw new UnsupportedOperationException("Only dispatcherServlet is supported");
        }

        @Override
        public ServletRegistration.Dynamic addServlet(String s, Class<? extends Servlet> aClass) {
            throw new UnsupportedOperationException("Only dispatcherServlet is supported");
        }

        @Override
        public ServletRegistration.Dynamic addServlet(String s, Servlet servlet) {
            if ("dispatcherServlet".equals(s)) {
                try {
                    servlet.init(new ServletConfig() {
                        @Override
                        public String getServletName() {
                            return s;
                        }

                        @Override
                        public ServletContext getServletContext() {
                            return SpringBootAwsServletContext.this;
                        }

                        @Override
                        public String getInitParameter(String name) {
                            return null;
                        }

                        @Override
                        public Enumeration<String> getInitParameterNames() {
                            return new Enumeration<String>() {
                                @Override
                                public boolean hasMoreElements() {
                                    return false;
                                }

                                @Override
                                public String nextElement() {
                                    return null;
                                }
                            };
                        }
                    });
                } catch (ServletException e) {
                    throw new RuntimeException("Cannot add servlet " + servlet, e);
                }
                return new ServletRegistration.Dynamic() {
                    @Override
                    public String getName() {
                        return s;
                    }

                    @Override
                    public String getClassName() {
                        return null;
                    }

                    @Override
                    public boolean setInitParameter(String name, String value) {
                        return false;
                    }

                    @Override
                    public String getInitParameter(String name) {
                        return null;
                    }

                    @Override
                    public Set<String> setInitParameters(Map<String, String> initParameters) {
                        return null;
                    }

                    @Override
                    public Map<String, String> getInitParameters() {
                        return null;
                    }

                    @Override
                    public Set<String> addMapping(String... urlPatterns) {
                        return null;
                    }

                    @Override
                    public Collection<String> getMappings() {
                        return null;
                    }

                    @Override
                    public String getRunAsRole() {
                        return null;
                    }

                    @Override
                    public void setAsyncSupported(boolean isAsyncSupported) {

                    }

                    @Override
                    public void setLoadOnStartup(int loadOnStartup) {

                    }

                    @Override
                    public Set<String> setServletSecurity(ServletSecurityElement constraint) {
                        return null;
                    }

                    @Override
                    public void setMultipartConfig(MultipartConfigElement multipartConfig) {

                    }

                    @Override
                    public void setRunAsRole(String roleName) {

                    }
                };
            } else {
                throw new UnsupportedOperationException("Only dispatcherServlet is supported");
            }
        }
    }
}
