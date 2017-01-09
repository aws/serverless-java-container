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
import com.amazonaws.serverless.proxy.internal.*;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.services.lambda.runtime.Context;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import javax.servlet.ServletContext;
import java.util.concurrent.CountDownLatch;

/**
 * Spring implementation of the `LambdaContainerHandler` abstract class. This class uses the `LambdaSpringApplicationInitializer`
 * object behind the scenes to proxy requests. The default implementation leverages the `AwsProxyHttpServletRequest` and
 * `AwsHttpServletResponse` implemented in the `aws-serverless-java-container-core` package.
 * @param <RequestType> The incoming event type
 * @param <ResponseType> The expected return type
 */
public class SpringLambdaContainerHandler<RequestType, ResponseType> extends LambdaContainerHandler<RequestType, ResponseType, AwsProxyHttpServletRequest, AwsHttpServletResponse> {
    private LambdaSpringApplicationInitializer initializer;

    // State vars
    private boolean initialized;


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
        initializer = new LambdaSpringApplicationInitializer(applicationContext);
    }

    @Override
    protected AwsHttpServletResponse getContainerResponse(CountDownLatch latch) {
        return new AwsHttpServletResponse(latch);
    }

    @Override
    protected void handleRequest(AwsProxyHttpServletRequest containerRequest, AwsHttpServletResponse containerResponse, Context lambdaContext) throws Exception {
        // wire up the application context on the first invocation
        if (!initialized) {
            ServletContext context = containerRequest.getServletContext();
            initializer.onStartup(context);
            initialized = true;
        }

        initializer.dispatch(containerRequest, containerResponse);
    }
}
