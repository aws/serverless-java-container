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


import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.internal.model.ContainerConfig;
import com.amazonaws.services.lambda.runtime.Context;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.SecurityContext;

import java.io.IOException;
import java.io.InputStream;


/**
 * Implementations of the RequestReader object are used by container objects to transform the incoming Lambda event into
 * a request object that can be used by the underlying container. Developers can build custom implementation of this interface
 * to support other container types as well as custom event types. This library provides a default implementation of this
 * object that supports requests for the AWS_PROXY integration.
 *
 * @param <RequestType> The type for the AWS Lambda event
 * @param <ContainerRequestType> The type for the undelying container request object
 */
public abstract class RequestReader<RequestType, ContainerRequestType> {

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    /**
     * The key for the <strong>API Gateway context</strong> property in the PropertiesDelegate object
     */
    public static final String API_GATEWAY_CONTEXT_PROPERTY = "com.amazonaws.apigateway.request.context";

    /**
     * The key for the <strong>API Gateway stage variables</strong> property in the PropertiesDelegate object
     */
    public static final String API_GATEWAY_STAGE_VARS_PROPERTY = "com.amazonaws.apigateway.stage.variables";

    /**
     * The key for the <strong>AWS Lambda context</strong> property in the PropertiesDelegate object
     */
    public static final String LAMBDA_CONTEXT_PROPERTY = "com.amazonaws.lambda.context";


    //-------------------------------------------------------------
    // Methods - Abstract
    //-------------------------------------------------------------

    /**
     * Reads the incoming event object and produces a populated request for the underlying container
     * @param request The incoming request object
     * @param securityContext A jax-rs SecurityContext object (@see com.amazonaws.serverless.proxy.internal.SecurityContextWriter)
     * @param lambdaContext The AWS Lambda context for the request
     * @return A valid request object for the underlying container
     * @throws InvalidRequestEventException This exception is thrown if anything goes wrong during the creation of the request object
     */
    protected abstract ContainerRequestType readRequest(RequestType request, SecurityContext securityContext, Context lambdaContext, ContainerConfig config)
            throws InvalidRequestEventException;


    protected abstract Class<? extends RequestType> getRequestClass();


    //-------------------------------------------------------------
    // Methods - Protected
    //-------------------------------------------------------------

    /**
     * Strips the base path from the request path if the container configuration object requires it
     * @param requestPath The incoming request path
     * @param config The container configuration object
     * @return The final request path
     */
    protected String stripBasePath(String requestPath, ContainerConfig config) {
        if (!config.isStripBasePath()) {
            return requestPath;
        }

        if (requestPath.startsWith(config.getServiceBasePath())) {
            String newRequestPath = requestPath.replaceFirst(config.getServiceBasePath(), "");
            if (!newRequestPath.startsWith("/")) {
                newRequestPath = "/" + newRequestPath;
            }
            return newRequestPath;
        }

        return requestPath;
    }
}
