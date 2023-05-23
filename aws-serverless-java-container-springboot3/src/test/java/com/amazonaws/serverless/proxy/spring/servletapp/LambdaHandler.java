package com.amazonaws.serverless.proxy.spring.servletapp;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.InitializationWrapper;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LambdaHandler implements RequestHandler<AwsProxyRequestBuilder, AwsProxyResponse> {
    private static SpringBootLambdaContainerHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> handler;
    private static SpringBootLambdaContainerHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> httpApiHandler;
    private static SpringBootLambdaContainerHandler<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> albHandler;
    private String type;

    public LambdaHandler(String reqType) {
        type = reqType;
        try {
            switch (type) {
                case "API_GW":
                    handler = new SpringBootProxyHandlerBuilder<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>()
                            .defaultProxy()
                            .initializationWrapper(new InitializationWrapper())
                            .servletApplication()
                            .springBootApplication(ServletApplication.class)
                            .buildAndInitialize();
                    break;
                case "ALB":
                    albHandler = new SpringBootProxyHandlerBuilder<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent>()
                            .defaultAlbProxy()
                            .initializationWrapper(new InitializationWrapper())
                            .servletApplication()
                            .springBootApplication(ServletApplication.class)
                            .buildAndInitialize();


                case "HTTP_API":
                    httpApiHandler = new SpringBootProxyHandlerBuilder<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>()
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
        //return handler.proxy(awsProxyRequest.build(), context);
        switch (type) {
            case "API_GW":
                APIGatewayProxyResponseEvent response = handler.proxy(awsProxyRequest.build(), context);
                return apiGwyToProxyResponse(response);
            case "ALB":
                ApplicationLoadBalancerResponseEvent albResponse = albHandler.proxy(awsProxyRequest.toAlbRequest(), context);
                return albToProxyResponse(albResponse);
            case "HTTP_API":
                APIGatewayV2HTTPResponse httpResponse = httpApiHandler.proxy(awsProxyRequest.toHttpApiV2Request(), context);
                return httpApiToProxyResponse(httpResponse);
            default:
                throw new RuntimeException("Unknown request type: " + type);
        }
    }

    private AwsProxyResponse apiGwyToProxyResponse(APIGatewayProxyResponseEvent event) {
        AwsProxyResponse proxyResponse = new AwsProxyResponse();
        if (Objects.nonNull(event.getBody())) {
            proxyResponse.setBody(event.getBody());
        }

        if (Objects.nonNull(event.getIsBase64Encoded())) {
            proxyResponse.setBase64Encoded(event.getIsBase64Encoded());
        }

        if (Objects.nonNull(event.getStatusCode())) {
            proxyResponse.setStatusCode(event.getStatusCode());
        }

        if (Objects.nonNull(event.getHeaders())) {
            proxyResponse.setHeaders(event.getHeaders());
        }

        if (Objects.nonNull(event.getMultiValueHeaders())) {
            proxyResponse.setMultiValueHeaders(new Headers());

            for (Map.Entry<String, List<String>> header: event.getMultiValueHeaders().entrySet()) {
                proxyResponse.getMultiValueHeaders().put(header.getKey(), header.getValue());
            }
        }

        return proxyResponse;
    }

    private AwsProxyResponse httpApiToProxyResponse(APIGatewayV2HTTPResponse event) {
        AwsProxyResponse proxyResponse = new AwsProxyResponse();
        if (Objects.nonNull(event.getBody())) {
            proxyResponse.setBody(event.getBody());
        }

        proxyResponse.setBase64Encoded(event.getIsBase64Encoded());
        proxyResponse.setStatusCode(event.getStatusCode());

        if (Objects.nonNull(event.getHeaders())) {
            proxyResponse.setHeaders(event.getHeaders());
        }

        if (Objects.nonNull(event.getMultiValueHeaders())) {
            proxyResponse.setMultiValueHeaders(new Headers());

            for (Map.Entry<String, List<String>> header: event.getMultiValueHeaders().entrySet()) {
                proxyResponse.getMultiValueHeaders().put(header.getKey(), header.getValue());
            }
        }

        return proxyResponse;
    }

    private AwsProxyResponse albToProxyResponse(ApplicationLoadBalancerResponseEvent event) {
        AwsProxyResponse proxyResponse = new AwsProxyResponse();
        if (Objects.nonNull(event.getBody())) {
            proxyResponse.setBody(event.getBody());
        }

        proxyResponse.setBase64Encoded(event.getIsBase64Encoded());
        proxyResponse.setStatusCode(event.getStatusCode());

        if (Objects.nonNull(event.getHeaders())) {
            proxyResponse.setHeaders(event.getHeaders());
        }

        if (Objects.nonNull(event.getMultiValueHeaders())) {
            proxyResponse.setMultiValueHeaders(new Headers());

            for (Map.Entry<String, List<String>> header: event.getMultiValueHeaders().entrySet()) {
                proxyResponse.getMultiValueHeaders().put(header.getKey(), header.getValue());
            }
        }

        return proxyResponse;
    }

//    public APIGatewayV2HTTPResponse handleRequestV2(AwsProxyRequestBuilder awsProxyRequest, Context context) { // Only for HTTP_API
//        return httpApiHandler.proxy(awsProxyRequest.toHttpApiV2Request(), context);
//    }
}
