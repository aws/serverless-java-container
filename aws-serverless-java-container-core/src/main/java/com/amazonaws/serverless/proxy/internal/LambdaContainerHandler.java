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
package com.amazonaws.serverless.proxy.internal;


import com.amazonaws.serverless.proxy.LogFormatter;
import com.amazonaws.serverless.proxy.internal.servlet.ApacheCombinedServletLogFormatter;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.ExceptionHandler;
import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.ResponseWriter;
import com.amazonaws.serverless.proxy.SecurityContextWriter;
import com.amazonaws.services.lambda.runtime.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;

import java.util.concurrent.CountDownLatch;


/**
 * Abstract class that declares the basic methods and objects for implementations of <code>LambdaContainerHandler</code>.
 *
 * @param <RequestType> The expected request object. This is the model class that the event JSON is de-serialized to
 * @param <ResponseType> The expected Lambda function response object. Responses from the container will be written to this model object
 * @param <ContainerRequestType> The request type for the wrapped Java container
 * @param <ContainerResponseType> The response or response writer type for the wrapped Java container
 */
public abstract class LambdaContainerHandler<RequestType, ResponseType, ContainerRequestType, ContainerResponseType> {

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    public static final String SERVER_INFO = "aws-serverless-java-container";


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private RequestReader<RequestType, ContainerRequestType> requestReader;
    private ResponseWriter<ContainerResponseType, ResponseType> responseWriter;
    private SecurityContextWriter<RequestType> securityContextWriter;
    private ExceptionHandler<ResponseType> exceptionHandler;

    protected Context lambdaContext;
    protected LogFormatter<ContainerRequestType, ContainerResponseType> logFormatter;

    private Logger log = LoggerFactory.getLogger(LambdaContainerHandler.class);



    //-------------------------------------------------------------
    // Variables - Private - Static
    //-------------------------------------------------------------

    private static ContainerConfig config = ContainerConfig.defaultConfig();
    private static volatile ObjectMapper objectMapper;



    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    protected LambdaContainerHandler(RequestReader<RequestType, ContainerRequestType> requestReader,
                                     ResponseWriter<ContainerResponseType, ResponseType> responseWriter,
                                     SecurityContextWriter<RequestType> securityContextWriter,
                                     ExceptionHandler<ResponseType> exceptionHandler) {
        log.info("Starting Lambda Container Handler");
        this.requestReader = requestReader;
        this.responseWriter = responseWriter;
        this.securityContextWriter = securityContextWriter;
        this.exceptionHandler = exceptionHandler;
    }


    //-------------------------------------------------------------
    // Methods - Abstract
    //-------------------------------------------------------------

    protected abstract ContainerResponseType getContainerResponse(ContainerRequestType request, CountDownLatch latch);


    protected abstract void handleRequest(ContainerRequestType containerRequest, ContainerResponseType containerResponse, Context lambdaContext)
            throws Exception;


    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
        return objectMapper;
    }

    /**
     * Configures the library to strip a base path from incoming requests before passing them on to the wrapped
     * framework. This was added in response to issue #34 (https://github.com/awslabs/aws-serverless-java-container/issues/34).
     * When creating a base path mapping for custom domain names in API Gateway we want to be able to strip the base path
     * from the request - the underlying service may not recognize this path.
     * @param basePath The base path to be stripped from the request
     */
    public void stripBasePath(String basePath) {
        config.setStripBasePath(true);
        config.setServiceBasePath(basePath);
    }

    /**
     * Sets the formatter used to log request data in CloudWatch. By default this is set to use an Apache
     * combined log format based on the servlet request and response object {@link ApacheCombinedServletLogFormatter}.
     * @param formatter The log formatter object
     */
    public void setLogFormatter(LogFormatter<ContainerRequestType, ContainerResponseType> formatter) {
        this.logFormatter = formatter;
    }


    /**
     * Proxies requests to the underlying container given the incoming Lambda request. This method returns a populated
     * return object for the Lambda function.
     *
     * @param request The incoming Lambda request
     * @param context The execution context for the Lambda function
     * @return A valid response type
     */
    public ResponseType proxy(RequestType request, Context context) {
        lambdaContext = context;
        try {
            SecurityContext securityContext = securityContextWriter.writeSecurityContext(request, context);
            CountDownLatch latch = new CountDownLatch(1);
            ContainerRequestType containerRequest = requestReader.readRequest(request, securityContext, context, config);
            ContainerResponseType containerResponse = getContainerResponse(containerRequest, latch);

            handleRequest(containerRequest, containerResponse, context);

            latch.await();

            if (logFormatter != null) {
                log.info(SecurityUtils.crlf(logFormatter.format(containerRequest, containerResponse, securityContext)));
            }

            return responseWriter.writeResponse(containerResponse, context);
        } catch (Exception e) {
            log.error("Error while handling request", e);

            return exceptionHandler.handle(e);
        }
    }


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    /**
     * Returns the current container configuration object.
     * @return The container configuration object
     */
    public static ContainerConfig getContainerConfig() {
        return config;
    }
}
