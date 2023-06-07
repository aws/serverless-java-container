package com.amazonaws.serverless.proxy.spring.webfluxapp;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.InitializationWrapper;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

public class LambdaHandler implements RequestHandler<AwsProxyRequestBuilder, APIGatewayProxyResponseEvent> {
    private static SpringBootLambdaContainerHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> handler;
    private static SpringBootLambdaContainerHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> httpApiHandler;

    private String type;

    public LambdaHandler(String reqType) {
        type = reqType;
        try {
            switch (type) {
                case "API_GW":
                //case "ALB": TODO: Check later
                    handler = new SpringBootProxyHandlerBuilder<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>()
                            .defaultProxy()
                            .initializationWrapper(new InitializationWrapper())
                            .springBootApplication(WebFluxTestApplication.class)
                            .buildAndInitialize();
                    break;
                case "HTTP_API":
                    httpApiHandler = new SpringBootProxyHandlerBuilder<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>()
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
    public APIGatewayProxyResponseEvent handleRequest(AwsProxyRequestBuilder awsProxyRequest, Context context) {
        return handler.proxy(awsProxyRequest.build(), context);
//        switch (type) {
//            case "API_GW":
//                return handler.proxy(awsProxyRequest.build(), context);
//            case "ALB":
//                return handler.proxy(awsProxyRequest.alb().build(), context);
//            case "HTTP_API":
//                return httpApiHandler.proxy(awsProxyRequest.toHttpApiV2Request(), context);
//            default:
//                throw new RuntimeException("Unknown request type: " + type);
//        }
    }

    public APIGatewayV2HTTPResponse handleRequestV2(AwsProxyRequestBuilder awsProxyRequest, Context context) {
        return httpApiHandler.proxy(awsProxyRequest.toHttpApiV2Request(), context);
    }
}
