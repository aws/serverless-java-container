package com.amazonaws.serverless.proxy.spring.springslowapp;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.AwsProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayProxyRequestEvent;

import java.time.Instant;

public class LambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, AwsProxyResponseEvent> {
    private SpringLambdaContainerHandler<APIGatewayProxyRequestEvent, AwsProxyResponseEvent> handler;
    private long constructorTime;

    public LambdaHandler() throws ContainerInitializationException {
        long startTime = Instant.now().toEpochMilli();
        handler = new SpringProxyHandlerBuilder<APIGatewayProxyRequestEvent>()
                .defaultProxy()
                .asyncInit()
                .configurationClasses(SlowAppConfig.class)
                .buildAndInitialize();
        constructorTime = Instant.now().toEpochMilli() - startTime;
    }

    public long getConstructorTime() {
        return constructorTime;
    }

    @Override
    public AwsProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent awsProxyRequest, Context context) {
        return handler.proxy(awsProxyRequest, context);
    }
}
