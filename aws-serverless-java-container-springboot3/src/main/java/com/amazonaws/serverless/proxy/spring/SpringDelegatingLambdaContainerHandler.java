package com.amazonaws.serverless.proxy.spring;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayV2HTTPEvent;
import org.springframework.cloud.function.serverless.web.FunctionClassUtils;
import org.springframework.cloud.function.serverless.web.ProxyHttpServletRequest;
import org.springframework.cloud.function.serverless.web.ProxyMvc;
import org.springframework.util.StringUtils;

import com.amazonaws.serverless.proxy.AwsHttpApiV2SecurityContextWriter;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.SecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

/**
 * An implementation of {@link RequestStreamHandler} which delegates to
 * Spring Cloud Function serverless web module managed by Spring team.
 *
 * It requires no sub-classing from the user other then being identified as "Handler".
 * The configuration class(es) should be provided via MAIN_CLASS environment variable.
 *
 */
public class SpringDelegatingLambdaContainerHandler implements RequestStreamHandler {

    private final Class<?>[] startupClasses;

    private final ProxyMvc mvc;

    private final ObjectMapper mapper;

    private final AwsProxyHttpServletResponseWriter responseWriter;

    public SpringDelegatingLambdaContainerHandler() {
        this(new Class[] {FunctionClassUtils.getStartClass()});
    }

    public SpringDelegatingLambdaContainerHandler(Class<?>... startupClasses) {
        this.startupClasses = startupClasses;
        this.mvc = ProxyMvc.INSTANCE(this.startupClasses);
        this.mapper = new ObjectMapper();
        this.responseWriter = new AwsProxyHttpServletResponseWriter();
    }

    @SuppressWarnings({"rawtypes" })
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context lambdaContext) throws IOException {
        Map request = mapper.readValue(input, Map.class);
        SecurityContextWriter securityWriter = "2.0".equals(request.get("version"))
                ? new AwsHttpApiV2SecurityContextWriter() : new AwsProxySecurityContextWriter();
        HttpServletRequest httpServletRequest = "2.0".equals(request.get("version"))
                ? this.generateRequest2(request, lambdaContext, securityWriter) : this.generateRequest(request, lambdaContext, securityWriter);

        CountDownLatch latch = new CountDownLatch(1);
        AwsHttpServletResponse httpServletResponse = new AwsHttpServletResponse(httpServletRequest, latch);
        try {
            mvc.service(httpServletRequest, httpServletResponse);
            latch.await(10, TimeUnit.SECONDS);
            mapper.writeValue(output, responseWriter.writeResponse(httpServletResponse, lambdaContext));
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private HttpServletRequest generateRequest(Map request, Context lambdaContext, SecurityContextWriter securityWriter) {
        AwsProxyRequest v1Request = this.mapper.convertValue(request, AwsProxyRequest.class);

        ProxyHttpServletRequest httpRequest = new ProxyHttpServletRequest(this.mvc.getApplicationContext().getServletContext(),
                v1Request.getHttpMethod(), v1Request.getPath());

        if (StringUtils.hasText(v1Request.getBody())) {
            httpRequest.setContentType("application/json");
            httpRequest.setContent(v1Request.getBody().getBytes(StandardCharsets.UTF_8));
        }
        httpRequest.setAttribute(RequestReader.API_GATEWAY_CONTEXT_PROPERTY, v1Request.getRequestContext());
        httpRequest.setAttribute(RequestReader.API_GATEWAY_STAGE_VARS_PROPERTY, v1Request.getStageVariables());
        httpRequest.setAttribute(RequestReader.API_GATEWAY_EVENT_PROPERTY, v1Request);
        httpRequest.setAttribute(RequestReader.ALB_CONTEXT_PROPERTY, v1Request.getRequestContext().getElb());
        httpRequest.setAttribute(RequestReader.LAMBDA_CONTEXT_PROPERTY, lambdaContext);
        httpRequest.setAttribute(RequestReader.JAX_SECURITY_CONTEXT_PROPERTY, securityWriter.writeSecurityContext(v1Request, lambdaContext));
        return httpRequest;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public HttpServletRequest generateRequest2(Map request, Context lambdaContext, SecurityContextWriter securityWriter) {
        APIGatewayV2HTTPEvent v2Request = this.mapper.convertValue(request, APIGatewayV2HTTPEvent.class);
        ProxyHttpServletRequest httpRequest = new ProxyHttpServletRequest(this.mvc.getApplicationContext().getServletContext(),
                v2Request.getRequestContext().getHttp().getMethod(), v2Request.getRequestContext().getHttp().getPath());

        if (StringUtils.hasText(v2Request.getBody())) {
            httpRequest.setContentType("application/json");
            httpRequest.setContent(v2Request.getBody().getBytes(StandardCharsets.UTF_8));
        }
        httpRequest.setAttribute(RequestReader.HTTP_API_CONTEXT_PROPERTY, v2Request.getRequestContext());
        httpRequest.setAttribute(RequestReader.HTTP_API_STAGE_VARS_PROPERTY, v2Request.getStageVariables());
        httpRequest.setAttribute(RequestReader.HTTP_API_EVENT_PROPERTY, v2Request);
        httpRequest.setAttribute(RequestReader.LAMBDA_CONTEXT_PROPERTY, lambdaContext);
        httpRequest.setAttribute(RequestReader.JAX_SECURITY_CONTEXT_PROPERTY, securityWriter.writeSecurityContext(v2Request, lambdaContext));
        return httpRequest;
    }
}
