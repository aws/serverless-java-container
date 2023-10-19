package com.amazonaws.serverless.proxy.spring.servletapp;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.InitializationWrapper;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletRequestHelper;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayV2HTTPEvent;
import jakarta.ws.rs.core.HttpHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LambdaStreamHandler implements RequestStreamHandler {
    private static SpringBootLambdaContainerHandler<APIGatewayProxyRequestEvent, AwsProxyResponse> handler;
    private static SpringBootLambdaContainerHandler<APIGatewayV2HTTPEvent, AwsProxyResponse> httpApiHandler;
    private static SpringBootLambdaContainerHandler<ApplicationLoadBalancerRequestEvent, AwsProxyResponse> albHandler;
    private String type;

    public LambdaStreamHandler(String reqType) {
        type = reqType;
        try {
            switch (type) {
                case "API_GW":
                    handler = new SpringBootProxyHandlerBuilder<APIGatewayProxyRequestEvent>()
                            .defaultProxy()
                            .initializationWrapper(new InitializationWrapper())
                            .servletApplication()
                            .springBootApplication(ServletApplication.class)
                            .buildAndInitialize();
                    break;
                case "ALB":
                    albHandler = new SpringBootProxyHandlerBuilder<ApplicationLoadBalancerRequestEvent>()
                            .defaultAlbProxy()
                            .initializationWrapper(new InitializationWrapper())
                            .servletApplication()
                            .springBootApplication(ServletApplication.class)
                            .buildAndInitialize();
                    break;
                case "HTTP_API":
                    httpApiHandler = new SpringBootProxyHandlerBuilder<APIGatewayV2HTTPEvent>()
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
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        switch (type) {
            case "API_GW":
                handler.proxyStream(inputStream, outputStream, context);
                break;
            case "ALB":
                albHandler.proxyStream(inputStream, outputStream, context);
                break;
            case "HTTP_API":
                httpApiHandler.proxyStream(inputStream, outputStream, context);
        }

    }
}
