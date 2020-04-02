package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.servlet.ServletLambdaContainerHandlerBuilder;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import org.springframework.boot.WebApplicationType;

import javax.servlet.http.HttpServletRequest;

public final class SpringBootProxyHandlerBuilder<RequestType> extends ServletLambdaContainerHandlerBuilder<
        RequestType,
            AwsProxyResponse,
            HttpServletRequest,
            SpringBootLambdaContainerHandler<RequestType, AwsProxyResponse>,
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
    public SpringBootLambdaContainerHandler<RequestType, AwsProxyResponse> build() throws ContainerInitializationException {
        validate();
        if (springBootInitializer == null) {
            throw new ContainerInitializationException("Missing spring boot application class in builder", null);
        }
        SpringBootLambdaContainerHandler<RequestType, AwsProxyResponse> handler =  new SpringBootLambdaContainerHandler<RequestType, AwsProxyResponse>(
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
    public SpringBootLambdaContainerHandler<RequestType, AwsProxyResponse> buildAndInitialize() throws ContainerInitializationException {
        SpringBootLambdaContainerHandler<RequestType, AwsProxyResponse> handler = build();
        initializationWrapper.start(handler);
        return handler;
    }
}
