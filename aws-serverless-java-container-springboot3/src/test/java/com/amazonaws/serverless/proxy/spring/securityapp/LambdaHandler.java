package com.amazonaws.serverless.proxy.spring.securityapp;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.AwsProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayProxyRequestEvent;

public class LambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, AwsProxyResponseEvent> {
    private static SpringBootLambdaContainerHandler<APIGatewayProxyRequestEvent, AwsProxyResponseEvent> handler;

    static {
        try {
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(SecurityApplication.class);
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public AwsProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent awsProxyRequest, Context context) {
        return handler.proxy(awsProxyRequest, context);
    }
}
