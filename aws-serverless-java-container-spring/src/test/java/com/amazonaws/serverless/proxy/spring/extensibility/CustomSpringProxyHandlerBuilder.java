package com.amazonaws.serverless.proxy.spring.extensibility;

import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.events.AwsProxyResponseEvent;
import org.springframework.web.context.ConfigurableWebApplicationContext;

public class CustomSpringProxyHandlerBuilder<RequestType> extends SpringProxyHandlerBuilder<RequestType> {

    @Override
    protected SpringLambdaContainerHandler<RequestType, AwsProxyResponseEvent> createHandler(ConfigurableWebApplicationContext ctx) {
        return new CustomSpringLambdaContainerHandler<>(requestTypeClass, responseTypeClass, requestReader, responseWriter,
                securityContextWriter, exceptionHandler, ctx, initializationWrapper);
    }
}
