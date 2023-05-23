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
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import jakarta.servlet.http.HttpServletRequest;

public class SpringProxyHandlerBuilder<RequestType, ResponseType> extends ServletLambdaContainerHandlerBuilder<
            RequestType,
            ResponseType,
            HttpServletRequest,
            SpringLambdaContainerHandler<RequestType, ResponseType>,
            SpringProxyHandlerBuilder<RequestType, ResponseType>> {
    private ConfigurableWebApplicationContext springContext;
    private Class[] configurationClasses;
    private String[] profiles;

    @Override
    protected SpringProxyHandlerBuilder<RequestType, ResponseType> self() {
        return this;
    }


    public SpringProxyHandlerBuilder<RequestType, ResponseType> springApplicationContext(ConfigurableWebApplicationContext app) {
        springContext = app;
        return self();
    }

    public SpringProxyHandlerBuilder<RequestType, ResponseType> configurationClasses(Class... config) {
        configurationClasses = config;
        return self();
    }

    public SpringProxyHandlerBuilder<RequestType, ResponseType> profiles(String... profiles) {
        this.profiles = profiles;
        return self();
    }

    @Override
    public SpringLambdaContainerHandler<RequestType, ResponseType> build() throws ContainerInitializationException {
        validate();
        if (springContext == null && (configurationClasses == null || configurationClasses.length == 0)) {
            throw new ContainerInitializationException("Missing both configuration classes and application context, at least" +
                    " one of the two must be populated", null);
        }
        ConfigurableWebApplicationContext ctx = springContext;
        if (ctx == null) {
            ctx = new AnnotationConfigWebApplicationContext();
            if (configurationClasses != null) {
                ((AnnotationConfigWebApplicationContext)ctx).register(configurationClasses);
            }
        }

        SpringLambdaContainerHandler<RequestType, ResponseType> handler = createHandler(ctx);
        if (profiles != null) {
            handler.activateSpringProfiles(profiles);
        }
        return handler;
    }

    protected SpringLambdaContainerHandler<RequestType, ResponseType> createHandler(ConfigurableWebApplicationContext ctx) {
        return new SpringLambdaContainerHandler<>(
                requestTypeClass, responseTypeClass, requestReader, responseWriter,
                securityContextWriter, exceptionHandler, ctx, initializationWrapper
        );
    }

    @Override
    public SpringLambdaContainerHandler<RequestType, ResponseType> buildAndInitialize() throws ContainerInitializationException {
        SpringLambdaContainerHandler<RequestType, ResponseType> handler = build();
        initializationWrapper.start(handler);
        return handler;
    }
}
