package com.amazonaws.serverless.proxy.spring;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.serverless.proxy.InitializationTypeHelper;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import org.springframework.cloud.function.serverless.web.FunctionClassUtils;
import org.springframework.cloud.function.serverless.web.ServerlessMVC;

import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
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

    private final ServerlessMVC mvc;

    private final ObjectMapper mapper;

    private final AwsProxyHttpServletResponseWriter responseWriter;

    public SpringDelegatingLambdaContainerHandler() {
        this(new Class[] {FunctionClassUtils.getStartClass()});
    }

    public SpringDelegatingLambdaContainerHandler(Class<?>... startupClasses) {
        this.startupClasses = startupClasses;
        this.mvc = ServerlessMVC.INSTANCE(this.startupClasses);
        if (InitializationTypeHelper.isAsyncInitializationDisabled()) {
    		mvc.waitForContext();
    	}
        this.mapper = new ObjectMapper();
        this.responseWriter = new AwsProxyHttpServletResponseWriter();
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context lambdaContext) throws IOException {
        HttpServletRequest httpServletRequest = AwsSpringHttpProcessingUtils
        		.generateHttpServletRequest(input, lambdaContext, this.mvc.getServletContext(), this.mapper);
        AwsProxyResponse awsProxyResponse = AwsSpringHttpProcessingUtils.processRequest(httpServletRequest, mvc, responseWriter);
        this.mapper.writeValue(output, awsProxyResponse);
    }
}
