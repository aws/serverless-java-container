package com.amazonaws.serverless.proxy.spring.jpaapp;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.InitializationWrapper;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.model.VPCLatticeV2RequestEvent;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaHandler implements RequestHandler<AwsProxyRequestBuilder, AwsProxyResponse> {
    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    private static SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> httpApiHandler;
    private static SpringBootLambdaContainerHandler<VPCLatticeV2RequestEvent, AwsProxyResponse> latticeV2Handler;
    private String type;

    public LambdaHandler(String reqType) {
        type = reqType;
        try {
            switch (type) {
                case "API_GW":
                case "ALB":
                    handler = new SpringBootProxyHandlerBuilder<AwsProxyRequest>()
                                .defaultProxy()
                                .initializationWrapper(new InitializationWrapper())
                                .servletApplication()
                                .springBootApplication(JpaApplication.class)
                                .buildAndInitialize();
                    break;
                case "HTTP_API":
                    httpApiHandler = new SpringBootProxyHandlerBuilder<HttpApiV2ProxyRequest>()
                            .defaultHttpApiV2Proxy()
                            .initializationWrapper(new InitializationWrapper())
                            .servletApplication()
                            .springBootApplication(JpaApplication.class)
                            .buildAndInitialize();
                    break;
                case "VPC_LATTICE_V2":
                    latticeV2Handler = new SpringBootProxyHandlerBuilder<VPCLatticeV2RequestEvent>()
                            .defaultVpcLatticeV2Proxy()
                            .initializationWrapper(new InitializationWrapper())
                            .servletApplication()
                            .springBootApplication(JpaApplication.class)
                            .buildAndInitialize();
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
            case "VPC_LATTICE_V2":
                return latticeV2Handler.proxy(awsProxyRequest.toVPCLatticeV2Request(), context);
            default:
                throw new RuntimeException("Unknown request type: " + type);
        }
    }
}
