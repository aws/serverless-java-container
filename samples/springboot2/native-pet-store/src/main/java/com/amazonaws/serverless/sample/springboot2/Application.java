package com.amazonaws.serverless.sample.springboot2;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.serverless.runtime.Runtime;
import com.amazonaws.serverless.sample.springboot2.controller.PetsController;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;


@SpringBootApplication(proxyBeanMethods = false)
@Import({ PetsController.class })
public class Application {
    private static ConfigurableApplicationContext ctx;

    public static void main(String[] args) throws ContainerInitializationException {
        SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler =
                new SpringBootProxyHandlerBuilder<AwsProxyRequest>()
                        .defaultProxy()
                        .servletApplication()
                        .springBootApplication(Application.class)
                        .buildAndInitialize();
        if (System.getProperty("agentrun") != null) {
            AwsProxyRequest testReq = new AwsProxyRequestBuilder("/pets", "GET").build();
            handler.proxy(testReq, new MockLambdaContext());
        } else {
            Runtime lambdaRuntime = new Runtime(handler);
            lambdaRuntime.start();
        }
    }
}