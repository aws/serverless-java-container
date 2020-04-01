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
package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.services.lambda.runtime.Context;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

/**
 * Simple implementation of the <code>RequestReader</code> interface that receives an <code>AwsProxyRequest</code>
 * object and uses it to initialize a <code>AwsProxyHttpServletRequest</code> object.
 */
public class AwsProxyHttpServletRequestReader extends RequestReader<AwsProxyRequest, HttpServletRequest> {
    static final String INVALID_REQUEST_ERROR = "The incoming event is not a valid request from Amazon API Gateway or an Application Load Balancer";

    private ServletContext servletContext;
    //-------------------------------------------------------------
    // Methods - Implementation
    //-------------------------------------------------------------

    public void setServletContext(ServletContext ctx) {
        servletContext = ctx;
    }

    @Override
    public HttpServletRequest readRequest(AwsProxyRequest request, SecurityContext securityContext, Context lambdaContext, ContainerConfig config)
            throws InvalidRequestEventException {
        // Expect the HTTP method and context to be populated. If they are not, we are handling an
        // unsupported event type.
        if (request.getHttpMethod() == null || request.getHttpMethod().equals("") || request.getRequestContext() == null) {
            throw new InvalidRequestEventException(INVALID_REQUEST_ERROR);
        }

        request.setPath(stripBasePath(request.getPath(), config));
        if (request.getMultiValueHeaders() != null && request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE) != null) {
            String contentType = request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            // put single as we always expect to have one and only one content type in a request.
            request.getMultiValueHeaders().putSingle(HttpHeaders.CONTENT_TYPE, getContentTypeWithCharset(contentType, config));
        }
        AwsProxyHttpServletRequest servletRequest = new AwsProxyHttpServletRequest(request, lambdaContext, securityContext, config);
        servletRequest.setServletContext(servletContext);
        servletRequest.setAttribute(API_GATEWAY_CONTEXT_PROPERTY, request.getRequestContext());
        servletRequest.setAttribute(API_GATEWAY_STAGE_VARS_PROPERTY, request.getStageVariables());
        servletRequest.setAttribute(API_GATEWAY_EVENT_PROPERTY, request);
        servletRequest.setAttribute(ALB_CONTEXT_PROPERTY, request.getRequestContext().getElb());
        servletRequest.setAttribute(LAMBDA_CONTEXT_PROPERTY, lambdaContext);
        servletRequest.setAttribute(JAX_SECURITY_CONTEXT_PROPERTY, securityContext);

        return servletRequest;
    }

    //-------------------------------------------------------------
    // Methods - Protected
    //-------------------------------------------------------------

    @Override
    protected Class<? extends AwsProxyRequest> getRequestClass() {
        return AwsProxyRequest.class;
    }

    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    private String getContentTypeWithCharset(String headerValue, ContainerConfig config) {
        if (headerValue == null || "".equals(headerValue.trim())) {
            return headerValue;
        }

        if (headerValue.contains("charset=")) {
            return headerValue;
        }

        String newValue = headerValue;
        if (!headerValue.trim().endsWith(";")) {
            newValue += "; ";
        }

        newValue += "charset=" + config.getDefaultContentCharset();
        return newValue;
    }
}
