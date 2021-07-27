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


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.*;
import com.amazonaws.serverless.proxy.internal.servlet.ApacheCombinedServletLogFormatter;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.services.lambda.runtime.Context;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


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
    private Class<RequestType> requestTypeClass;
    private Class<ResponseType> responseTypeClass;
    private InitializationWrapper initializationWrapper;

    protected Context lambdaContext;
    private LogFormatter<ContainerRequestType, ContainerResponseType> logFormatter;

    private Logger log = LoggerFactory.getLogger(LambdaContainerHandler.class);

    private ObjectReader objectReader;
    private ObjectWriter objectWriter;

    //-------------------------------------------------------------
    // Variables - Private - Static
    //-------------------------------------------------------------

    private static ContainerConfig config = ContainerConfig.defaultConfig();
    private static ObjectMapper objectMapper = new ObjectMapper();
    static {
        registerAfterBurner();
    }

    private static void registerAfterBurner() {
        objectMapper.registerModule(new AfterburnerModule());
    }


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    protected LambdaContainerHandler(Class<RequestType> requestClass,
                                     Class<ResponseType> responseClass,
                                     RequestReader<RequestType, ContainerRequestType> requestReader,
                                     ResponseWriter<ContainerResponseType, ResponseType> responseWriter,
                                     SecurityContextWriter<RequestType> securityContextWriter,
                                     ExceptionHandler<ResponseType> exceptionHandler,
                                     InitializationWrapper init) {
        log.info("Starting Lambda Container Handler");
        requestTypeClass = requestClass;
        responseTypeClass = responseClass;
        this.requestReader = requestReader;
        this.responseWriter = responseWriter;
        this.securityContextWriter = securityContextWriter;
        this.exceptionHandler = exceptionHandler;
        initializationWrapper = init;
        objectReader = getObjectMapper().readerFor(requestTypeClass);
        objectWriter = getObjectMapper().writerFor(responseTypeClass);

    }

    protected LambdaContainerHandler(Class<RequestType> requestClass,
                                     Class<ResponseType> responseClass,
                                     RequestReader<RequestType, ContainerRequestType> requestReader,
                                     ResponseWriter<ContainerResponseType, ResponseType> responseWriter,
                                     SecurityContextWriter<RequestType> securityContextWriter,
                                     ExceptionHandler<ResponseType> exceptionHandler) {
        this(requestClass, responseClass, requestReader, responseWriter, securityContextWriter, exceptionHandler, new InitializationWrapper());
    }


    //-------------------------------------------------------------
    // Methods - Abstract
    //-------------------------------------------------------------

    protected abstract ContainerResponseType getContainerResponse(ContainerRequestType request, CountDownLatch latch);


    protected abstract void handleRequest(ContainerRequestType containerRequest, ContainerResponseType containerResponse, Context lambdaContext)
            throws Exception;

    public abstract void initialize()
            throws ContainerInitializationException;

    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Returns the initialization wrapper this container handler will monitor to handle events
     * @return The initialization wrapper that was passed to the constructor and this instance will use to decide
     *         whether it can start handling events.
     */
    public InitializationWrapper getInitializationWrapper() {
        return initializationWrapper;
    }

    /**
     * Sets a new initialization wrapper.
     * @param wrapper The wrapper this instance will use to decide whether it can start handling events.
     */
    public void setInitializationWrapper(InitializationWrapper wrapper) {
        initializationWrapper = wrapper;
    }

    /**
     * Configures the library to strip a base path from incoming requests before passing them on to the wrapped
     * framework. This was added in response to issue #34 (https://github.com/awslabs/aws-serverless-java-container/issues/34).
     * When creating a base path mapping for custom domain names in API Gateway we want to be able to strip the base path
     * from the request - the underlying service may not recognize this path.
     * @param basePath The base path to be stripped from the request
     */
    public void stripBasePath(String basePath) {
        if (basePath == null || "".equals(basePath)) {
            config.setStripBasePath(false);
            config.setServiceBasePath(null);
        } else {
            config.setStripBasePath(true);
            config.setServiceBasePath(basePath);
        }
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
        CountDownLatch latch = new CountDownLatch(1);
        try {
            SecurityContext securityContext = securityContextWriter.writeSecurityContext(request, context);
            ContainerRequestType containerRequest = requestReader.readRequest(request, securityContext, context, config);
            ContainerResponseType containerResponse = getContainerResponse(containerRequest, latch);

            if (initializationWrapper != null && initializationWrapper.getInitializationLatch() != null) {
                // we let the potential InterruptedException bubble up
                if (!initializationWrapper.getInitializationLatch().await(config.getInitializationTimeout(), TimeUnit.MILLISECONDS)) {
                    throw new ContainerInitializationException("Could not initialize framework within the " + config.getInitializationTimeout() + "ms timeout", null);
                }
            }

            handleRequest(containerRequest, containerResponse, context);

            latch.await();

            if (logFormatter != null) {
                log.info(SecurityUtils.crlf(logFormatter.format(containerRequest, containerResponse, securityContext)));
            }

            return responseWriter.writeResponse(containerResponse, context);
        } catch (Exception e) {
            log.error("Error while handling request", e);
            // release all waiting threads. This is safe here because if the count was already 0
            // the latch will do nothing
            latch.countDown();

            if (getContainerConfig().isDisableExceptionMapper()) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            } else {
                return exceptionHandler.handle(e);
            }
        }
    }


    /**
     * Handles Lambda <code>RequestStreamHandler</code> method. The method uses an <code>ObjectMapper</code>
     * to transform the incoming input stream into the given {@link RequestType} and then calls the
     * {@link #proxy(Object, Context)} method to handle the request. The output from the proxy method is
     * written on the given output stream.
     * @param input Lambda's incoming input stream
     * @param output Lambda's response output stream
     * @param context Lambda's context object
     * @throws IOException If an error occurs during the stream processing
     */
    public void proxyStream(InputStream input, OutputStream output, Context context)
            throws IOException {

        try {
            RequestType request = objectReader.readValue(input);
            ResponseType resp = proxy(request, context);

            objectWriter.writeValue(output, resp);
        } catch (JsonParseException e) {
            log.error("Error while parsing request object stream", e);
            getObjectMapper().writeValue(output, exceptionHandler.handle(e));
        } catch (JsonMappingException e) {
            log.error("Error while mapping object to RequestType class", e);
            getObjectMapper().writeValue(output, exceptionHandler.handle(e));
        } finally {
            output.flush();
            output.close();
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
