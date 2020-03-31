package com.amazonaws.serverless.proxy.spring.springbootslowapp;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.time.Instant;


public class SBLambdaHandler
        implements RequestHandler<AwsProxyRequest, AwsProxyResponse>
{
    SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    private long constructorTime;

    public SBLambdaHandler() throws ContainerInitializationException {
        long startTime = Instant.now().toEpochMilli();
        handler = new SpringBootProxyHandlerBuilder()
                .defaultProxy()
                .asyncInit()
                .springBootApplication(TestApplication.class)
                .buildAndInitialize();
        constructorTime = Instant.now().toEpochMilli() - startTime;
    }

    public long getConstructorTime() {
        return constructorTime;
    }

    public AwsProxyResponse handleRequest(AwsProxyRequest awsProxyRequest, Context context)
    {
        return handler.proxy(awsProxyRequest, context);
    }
}

