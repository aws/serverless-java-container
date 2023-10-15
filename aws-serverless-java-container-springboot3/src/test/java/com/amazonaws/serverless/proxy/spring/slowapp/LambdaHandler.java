package com.amazonaws.serverless.proxy.spring.slowapp;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.AwsProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayProxyRequestEvent;

import java.time.Instant;

public class LambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, AwsProxyResponseEvent> {
    private SpringBootLambdaContainerHandler<APIGatewayProxyRequestEvent, AwsProxyResponseEvent> handler;
    private long constructorTime;

    public LambdaHandler() {
        try {
            long startTime = Instant.now().toEpochMilli();
            System.out.println("startCall: " + startTime);
            handler = new SpringBootProxyHandlerBuilder<APIGatewayProxyRequestEvent>()
                    .defaultProxy()
                    .asyncInit()
                    .springBootApplication(SlowTestApplication.class)
                    .buildAndInitialize();
            constructorTime = Instant.now().toEpochMilli() - startTime;
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
        }
    }

    public long getConstructorTime() {
        return constructorTime;
    }

    @Override
    public AwsProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent awsProxyRequest, Context context) {
        return handler.proxy(awsProxyRequest, context);
    }
}
