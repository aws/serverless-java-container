package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.servlet.ServletLambdaContainerHandlerBuilder;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;

public final class SpringBootProxyHandlerBuilder extends ServletLambdaContainerHandlerBuilder<
            AwsProxyRequest,
            AwsProxyResponse,
            AwsProxyHttpServletRequest,
            SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse>,
            SpringBootProxyHandlerBuilder> {
    private Class<?> springBootInitializer;
    private String[] profiles;

    @Override
    protected SpringBootProxyHandlerBuilder self() {
        return this;
    }


    public SpringBootProxyHandlerBuilder springBootApplication(Class<?> app) {
        springBootInitializer = app;
        return self();
    }

    public SpringBootProxyHandlerBuilder profiles(String... profiles) {
        this.profiles = profiles;
        return self();
    }

    @Override
    public SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> build() throws ContainerInitializationException {
        validate();
        if (springBootInitializer == null) {
            throw new ContainerInitializationException("Missing spring boot application class in builder", null);
        }
        SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler =  new SpringBootLambdaContainerHandler<>(
                requestTypeClass,
                responseTypeClass,
                requestReader,
                responseWriter,
                securityContextWriter,
                exceptionHandler,
                springBootInitializer,
                initializationWrapper
        );
        if (profiles != null) {
            handler.activateSpringProfiles(profiles);
        }
        return handler;
    }

    @Override
    public SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> buildAndInitialize() throws ContainerInitializationException {
        SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = build();
        initializationWrapper.start(handler);
        return handler;
    }
}
