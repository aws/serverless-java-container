package com.amazonaws.serverless.proxy.spring.webfluxapp;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.InitializationWrapper;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.AwsProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayV2HTTPEvent;

public class LambdaHandler implements RequestHandler<AwsProxyRequestBuilder, AwsProxyResponseEvent> {
    private static SpringBootLambdaContainerHandler<APIGatewayProxyRequestEvent, AwsProxyResponseEvent> handler;
    private static SpringBootLambdaContainerHandler<APIGatewayV2HTTPEvent, AwsProxyResponseEvent> httpApiHandler;
    private static SpringBootLambdaContainerHandler<ApplicationLoadBalancerRequestEvent, AwsProxyResponseEvent> albHandler;

    private String type;

    public LambdaHandler(String reqType) {
        type = reqType;
        try {
            switch (type) {
                case "API_GW":
                    handler = new SpringBootProxyHandlerBuilder<APIGatewayProxyRequestEvent>()
                            .defaultProxy()
                            .initializationWrapper(new InitializationWrapper())
                            .springBootApplication(WebFluxTestApplication.class)
                            .buildAndInitialize();
                    break;
                case "ALB":
                    albHandler = new SpringBootProxyHandlerBuilder<ApplicationLoadBalancerRequestEvent>()
                            .defaultAlbProxy()
                            .initializationWrapper(new InitializationWrapper())
                            .springBootApplication(WebFluxTestApplication.class)
                            .buildAndInitialize();
                    break;
                case "HTTP_API":
                    httpApiHandler = new SpringBootProxyHandlerBuilder<APIGatewayV2HTTPEvent>()
                            .defaultHttpApiV2Proxy()
                            .initializationWrapper(new InitializationWrapper())
                            .springBootApplication(WebFluxTestApplication.class)
                            .buildAndInitialize();
                    break;
            }
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public AwsProxyResponseEvent handleRequest(AwsProxyRequestBuilder awsProxyRequest, Context context) {
        switch (type) {
            case "API_GW":
                return handler.proxy(awsProxyRequest.build(), context);
            case "ALB":
                return albHandler.proxy(awsProxyRequest.toAlbRequest(), context);
            case "HTTP_API":
                return httpApiHandler.proxy(awsProxyRequest.toHttpApiV2Request(), context);
            default:
                throw new RuntimeException("Unknown request type: " + type);
        }
    }
}
