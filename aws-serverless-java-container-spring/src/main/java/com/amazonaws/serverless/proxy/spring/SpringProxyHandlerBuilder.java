package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.servlet.ServletLambdaContainerHandlerBuilder;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import javax.servlet.http.HttpServletRequest;

public final class SpringProxyHandlerBuilder<RequestType> extends ServletLambdaContainerHandlerBuilder<
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

        SpringLambdaContainerHandler<RequestType, AwsProxyResponse> handler =  new SpringLambdaContainerHandler<RequestType, AwsProxyResponse>(
                requestTypeClass,
                responseTypeClass,
                requestReader,
                responseWriter,
                securityContextWriter,
                exceptionHandler,
                ctx,
                initializationWrapper
        );
        if (profiles != null) {
            handler.activateSpringProfiles(profiles);
        }
        return handler;
    }

    @Override
    public SpringLambdaContainerHandler<RequestType, AwsProxyResponse> buildAndInitialize() throws ContainerInitializationException {
        SpringLambdaContainerHandler<RequestType, AwsProxyResponse> handler = build();
        initializationWrapper.start(handler);
        return handler;
    }
}
