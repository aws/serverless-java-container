package com.amazonaws.serverless.proxy.spring.servletapp;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.InitializationWrapper;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

public class LambdaHandler implements RequestHandler<AwsProxyRequestBuilder, AwsProxyResponse> {
    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    private static SpringBootLambdaContainerHandler<APIGatewayV2HTTPEvent, AwsProxyResponse> httpApiHandler;
    private String type;

    public LambdaHandler(String reqType) {
        type = reqType;
        try {
            switch (type) {
                case "API_GW":
                case "ALB":
                    handler = new SpringBootProxyHandlerBuilder<AwsProxyRequest, AwsProxyResponse>()
                                .defaultProxy()
                                .initializationWrapper(new InitializationWrapper())
                                .servletApplication()
                                .springBootApplication(ServletApplication.class)
                                .buildAndInitialize();
                    break;
                case "HTTP_API":
                    httpApiHandler = new SpringBootProxyHandlerBuilder<APIGatewayV2HTTPEvent, AwsProxyResponse>()
                            .defaultHttpApiV2Proxy()
                            .initializationWrapper(new InitializationWrapper())
                            .servletApplication()
                            .springBootApplication(ServletApplication.class)
                            .buildAndInitialize();
                    break;
            }
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public AwsProxyResponse handleRequest(AwsProxyRequestBuilder awsProxyRequest, Context context) {
        switch (type) {
            case "API_GW":
                return handler.proxy(awsProxyRequest.build(), context);
            case "ALB":
                return handler.proxy(awsProxyRequest.alb().build(), context);
            case "HTTP_API":
                return httpApiHandler.proxy(awsProxyRequest.toHttpApiV2Request(), context);
            default:
                throw new RuntimeException("Unknown request type: " + type);
        }
    }
}
