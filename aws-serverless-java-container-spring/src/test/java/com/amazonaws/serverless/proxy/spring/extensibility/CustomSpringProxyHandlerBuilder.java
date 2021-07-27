package com.amazonaws.serverless.proxy.spring.extensibility;

import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringProxyHandlerBuilder;
import org.springframework.web.context.ConfigurableWebApplicationContext;

public class CustomSpringProxyHandlerBuilder<RequestType> extends SpringProxyHandlerBuilder<RequestType> {

    @Override
    protected SpringLambdaContainerHandler<RequestType, AwsProxyResponse> createHandler(ConfigurableWebApplicationContext ctx) {
        return new CustomSpringLambdaContainerHandler<>(requestTypeClass, responseTypeClass, requestReader, responseWriter,
                securityContextWriter, exceptionHandler, ctx, initializationWrapper);
    }
}
