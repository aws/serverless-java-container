/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.SecurityContext;

public class AwsHttpApiV2HttpServletRequestReader extends RequestReader<HttpApiV2ProxyRequest, HttpServletRequest> {
    static final String INVALID_REQUEST_ERROR = "The incoming event is not a valid HTTP API v2 proxy request";

    @Override
    public HttpServletRequest readRequest(HttpApiV2ProxyRequest request, SecurityContext securityContext, Context lambdaContext, ContainerConfig config) throws InvalidRequestEventException {
        if (request.getRequestContext() == null || request.getRequestContext().getHttp().getMethod() == null || request.getRequestContext().getHttp().getMethod().equals("")) {
            throw new InvalidRequestEventException(INVALID_REQUEST_ERROR);
        }

        // clean out the request path based on the container config
        request.setRawPath(stripBasePath(request.getRawPath(), config));

        AwsHttpApiV2ProxyHttpServletRequest servletRequest = new AwsHttpApiV2ProxyHttpServletRequest(request, lambdaContext, securityContext, config);
        servletRequest.setAttribute(HTTP_API_CONTEXT_PROPERTY, request.getRequestContext());
        servletRequest.setAttribute(HTTP_API_STAGE_VARS_PROPERTY, request.getStageVariables());
        servletRequest.setAttribute(HTTP_API_EVENT_PROPERTY, request);
        servletRequest.setAttribute(LAMBDA_CONTEXT_PROPERTY, lambdaContext);
        servletRequest.setAttribute(JAX_SECURITY_CONTEXT_PROPERTY, securityContext);

        return servletRequest;
    }

    @Override
    protected Class<? extends HttpApiV2ProxyRequest> getRequestClass() {
        return HttpApiV2ProxyRequest.class;
    }
}
