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
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import javax.servlet.http.HttpServletRequest;

public class SpringProxyHandlerBuilder<RequestType> extends ServletLambdaContainerHandlerBuilder<
            RequestType,
            AwsProxyResponse,
            HttpServletRequest,
            SpringLambdaContainerHandler<RequestType, AwsProxyResponse>,
            SpringProxyHandlerBuilder<RequestType>> {
    private ConfigurableWebApplicationContext springContext;
    private Class[] configurationClasses;
    private String[] profiles;

    @Override
    protected SpringProxyHandlerBuilder<RequestType> self() {
        return this;
    }


    public SpringProxyHandlerBuilder<RequestType> springApplicationContext(ConfigurableWebApplicationContext app) {
        springContext = app;
        return self();
    }

    public SpringProxyHandlerBuilder<RequestType> configurationClasses(Class... config) {
        configurationClasses = config;
        return self();
    }

    public SpringProxyHandlerBuilder<RequestType> profiles(String... profiles) {
        this.profiles = profiles;
        return self();
    }

    @Override
    public SpringLambdaContainerHandler<RequestType, AwsProxyResponse> build() throws ContainerInitializationException {
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

        SpringLambdaContainerHandler<RequestType, AwsProxyResponse> handler = createHandler(ctx);
        if (profiles != null) {
            handler.activateSpringProfiles(profiles);
        }
        return handler;
    }

    protected SpringLambdaContainerHandler<RequestType, AwsProxyResponse> createHandler(ConfigurableWebApplicationContext ctx) {
        return new SpringLambdaContainerHandler<>(
                requestTypeClass, responseTypeClass, requestReader, responseWriter,
                securityContextWriter, exceptionHandler, ctx, initializationWrapper
        );
    }

    @Override
    public SpringLambdaContainerHandler<RequestType, AwsProxyResponse> buildAndInitialize() throws ContainerInitializationException {
        SpringLambdaContainerHandler<RequestType, AwsProxyResponse> handler = build();
        initializationWrapper.start(handler);
        return handler;
    }
}
