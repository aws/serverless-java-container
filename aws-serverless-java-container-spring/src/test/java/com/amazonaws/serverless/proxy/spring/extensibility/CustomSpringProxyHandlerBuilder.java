package com.amazonaws.serverless.proxy.spring.extensibility;

import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.springframework.web.context.ConfigurableWebApplicationContext;

public class CustomSpringProxyHandlerBuilder<RequestType, ResponseType> extends SpringProxyHandlerBuilder<RequestType, ResponseType> {

    @Override
    protected SpringLambdaContainerHandler<RequestType, ResponseType> createHandler(ConfigurableWebApplicationContext ctx) {
        return new CustomSpringLambdaContainerHandler<>(requestTypeClass, responseTypeClass, requestReader, responseWriter,
                securityContextWriter, exceptionHandler, ctx, initializationWrapper);
    }
}
