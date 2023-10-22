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
package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.servlet.ServletLambdaContainerHandlerBuilder;
import com.amazonaws.services.lambda.runtime.events.AwsProxyResponseEvent;
import org.springframework.boot.WebApplicationType;

import jakarta.servlet.http.HttpServletRequest;

public final class SpringBootProxyHandlerBuilder<RequestType> extends ServletLambdaContainerHandlerBuilder<
        RequestType,
            AwsProxyResponseEvent,
            HttpServletRequest,
            SpringBootLambdaContainerHandler<RequestType, AwsProxyResponseEvent>,
            SpringBootProxyHandlerBuilder<RequestType>> {
    private Class<?> springBootInitializer;
    private String[] profiles;
    private WebApplicationType applicationType = WebApplicationType.REACTIVE;

    @Override
    protected SpringBootProxyHandlerBuilder<RequestType> self() {
        return this;
    }


    public SpringBootProxyHandlerBuilder<RequestType> springBootApplication(Class<?> app) {
        springBootInitializer = app;
        return self();
    }

    public SpringBootProxyHandlerBuilder<RequestType> profiles(String... profiles) {
        this.profiles = profiles;
        return self();
    }

    public SpringBootProxyHandlerBuilder<RequestType> servletApplication() {
        this.applicationType = WebApplicationType.SERVLET;
        return self();
    }

    @Override
    public SpringBootLambdaContainerHandler<RequestType, AwsProxyResponseEvent> build() throws ContainerInitializationException {
        validate();
        if (springBootInitializer == null) {
            throw new ContainerInitializationException("Missing spring boot application class in builder", null);
        }
        SpringBootLambdaContainerHandler<RequestType, AwsProxyResponseEvent> handler =  new SpringBootLambdaContainerHandler<RequestType, AwsProxyResponseEvent>(
                requestTypeClass,
                responseTypeClass,
                requestReader,
                responseWriter,
                securityContextWriter,
                exceptionHandler,
                springBootInitializer,
                initializationWrapper,
                applicationType
        );
        if (profiles != null) {
            handler.activateSpringProfiles(profiles);
        }
        return handler;
    }

    @Override
    public SpringBootLambdaContainerHandler<RequestType, AwsProxyResponseEvent> buildAndInitialize() throws ContainerInitializationException {
        SpringBootLambdaContainerHandler<RequestType, AwsProxyResponseEvent> handler = build();
        initializationWrapper.start(handler);
        return handler;
    }
}
