package com.amazonaws.serverless.proxy.spring.springbootapp;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.springbootapp.TestApplication;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;


public class LambdaHandler
        implements RequestHandler<AwsProxyRequestBuilder, AwsProxyResponse>
{
    SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> httpApiHandler;
    private String type;

    public LambdaHandler(String reqType) {
        type = reqType;
        switch (type) {
            case "API_GW":
            case "ALB":
                try {
                    handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(TestApplication.class);
                } catch (ContainerInitializationException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                break;
            case "HTTP_API":
                try {
                    httpApiHandler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(TestApplication.class);
                } catch (ContainerInitializationException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
        }
    }

    public AwsProxyResponse handleRequest(AwsProxyRequestBuilder awsProxyRequest, Context context)
    {
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

