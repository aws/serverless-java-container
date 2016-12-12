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
package com.amazonaws.serverless.proxy.jersey;


import com.amazonaws.serverless.proxy.internal.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.internal.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.ExceptionHandler;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.RequestReader;
import com.amazonaws.serverless.proxy.internal.ResponseWriter;
import com.amazonaws.serverless.proxy.internal.SecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;

import javax.ws.rs.core.Application;

import java.util.concurrent.CountDownLatch;


/**
 * Jersey-specific implementation of the <code>LambdaContainerHandler</code> interface. Given a Jax-Rs application
 * starts Jersey's <code>ApplicationHandler</code> and proxies requests and responses using the RequestReader and
 * ResponseWriter objects. The reader and writer objects are inherited from the <code>BaseLambdaContainerHandler</code>
 * object.
 *
 * <pre>
 * {@code
 *   public class LambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
 *     private ResourceConfig jerseyApplication = new ResourceConfig().packages("your.app.package");
 *     private JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> container = JerseyLambdaContainerHandler.getAwsProxyHandler(jerseyApplication);
 *
 *     public AwsProxyResponse handleRequest(AwsProxyRequest awsProxyRequest, Context context) {
 *       return container.proxy(awsProxyRequest, context);
 *     }
 *   }
 * }
 * </pre>
 *
 * @see com.amazonaws.serverless.proxy.internal.LambdaContainerHandler
 *
 * @param <RequestType> The type for the incoming Lambda event
 * @param <ResponseType> The type for Lambda's return value
 */
public class JerseyLambdaContainerHandler<RequestType, ResponseType> extends LambdaContainerHandler<RequestType, ResponseType, ContainerRequest, JerseyResponseWriter>
        implements Container {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    // The Jersey application object
    private Application jaxRsApplication;
    // The Jersey app handler to route requests
    private ApplicationHandler applicationHandler;


    //-------------------------------------------------------------
    // Methods - Public - Static
    //-------------------------------------------------------------

    /**
     * Returns an initialized <code>JerseyLambdaContainerHandler</code> that includes <code>RequestReader</code> and
     * <code>ResponseWriter</code> objects for the <code>AwsProxyRequest</code> and <code>AwsProxyResponse</code>
     * objects.
     *
     * @param jaxRsApplication A configured Jax-Rs application object. For Jersey apps this can be the default
     *                         <code>ResourceConfig</code> object
     * @return A <code>JerseyLambdaContainerHandler</code> object
     */
    public static JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(Application jaxRsApplication) {
        return new JerseyLambdaContainerHandler<>(new JerseyAwsProxyRequestReader(),
                                                  new JerseyAwsProxyResponseWriter(),
                                                  new AwsProxySecurityContextWriter(),
                                                  new AwsProxyExceptionHandler(),
                                                  jaxRsApplication);
    }


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    /**
     * Private constructor for a LambdaContainer. Sets the application object, sets the ApplicationHandler,
     * and initializes the application using the <code>onStartup</code> method.
     * @param jaxRsApplication A Jersey application instance.
     */
    public JerseyLambdaContainerHandler(RequestReader<RequestType, ContainerRequest> requestReader,
                                        ResponseWriter<JerseyResponseWriter, ResponseType> responseWriter,
                                        SecurityContextWriter<RequestType> securityContextWriter,
                                        ExceptionHandler<ResponseType> exceptionHandler,
                                        Application jaxRsApplication) {
        super(requestReader, responseWriter, securityContextWriter, exceptionHandler);

        this.jaxRsApplication = jaxRsApplication;
        this.applicationHandler = new ApplicationHandler(jaxRsApplication);

        applicationHandler.onStartup(this);
    }


    //-------------------------------------------------------------
    // Implementation - Container
    //-------------------------------------------------------------

    /**
     * Gets the configuration currently set in the internal <code>ApplicationHandler</code>
     *
     * @return The Jersey's ResourceConfig object currently running in the container
     */
    public ResourceConfig getConfiguration() {
        return applicationHandler.getConfiguration();
    }


    /**
     * The instantiated <code>ApplicationHandler</code> object used by this container
     *
     * @return Jersey's ApplicationHander object
     */
    public ApplicationHandler getApplicationHandler() {
        return applicationHandler;
    }


    /**
     * Shuts down and restarts the application handler in the current container. The <code>ApplicationHandler</code>
     * object is re-initialized with the <code>Application</code> object initially set in the <code>LambdaContainer.getInstance()</code>
     * call.
     */
    public void reload() {
        applicationHandler.onShutdown(this);

        this.applicationHandler = new ApplicationHandler(jaxRsApplication);

        applicationHandler.onReload(this);
        applicationHandler.onStartup(this);
    }


    /**
     * Restarts the application handler and configures a different <code>Application</code> object. The new application
     * resets the one currently configured in the container.
     * @param resourceConfig An initialized Application
     */
    public void reload(ResourceConfig resourceConfig) {
        applicationHandler.onShutdown(this);

        this.jaxRsApplication = resourceConfig;
        this.applicationHandler = new ApplicationHandler(resourceConfig);

        applicationHandler.onReload(this);
        applicationHandler.onStartup(this);
    }


    //-------------------------------------------------------------
    // Methods - Implementation
    //-------------------------------------------------------------

    @Override
    protected JerseyResponseWriter getContainerResponse(CountDownLatch latch) {
        return new JerseyResponseWriter(latch);
    }


    @Override
    protected void handleRequest(ContainerRequest containerRequest, JerseyResponseWriter jerseyResponseWriter) {
        containerRequest.setWriter(jerseyResponseWriter);

        applicationHandler.handle(containerRequest);
    }
}
