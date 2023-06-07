package com.amazonaws.serverless.proxy.spring.securityapp;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class LambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static SpringBootLambdaContainerHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> handler;

    static {
        try {
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(SecurityApplication.class);
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent awsProxyRequest, Context context) {
        return handler.proxy(awsProxyRequest, context);
    }
}
