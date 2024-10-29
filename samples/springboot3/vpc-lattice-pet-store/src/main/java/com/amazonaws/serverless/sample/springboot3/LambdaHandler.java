package com.amazonaws.serverless.sample.springboot3;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.VPCLatticeV2RequestEvent;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumSet;


public class LambdaHandler implements RequestHandler<VPCLatticeV2RequestEvent, AwsProxyResponse> {

    private static final Logger log = LoggerFactory.getLogger(LambdaHandler.class);
    private static SpringBootLambdaContainerHandler<VPCLatticeV2RequestEvent, AwsProxyResponse> handler;
    static {
        try {
            handler = SpringBootLambdaContainerHandler.getVpcLatticeV2Handler(Application.class);
        } catch (ContainerInitializationException e) {
            // if we fail here. We re-throw the exception to force another cold start
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public AwsProxyResponse handleRequest(VPCLatticeV2RequestEvent event, Context context) {
        return handler.proxy(event, context);
    }
}
