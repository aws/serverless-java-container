package com.amazonaws.serverless.proxy.spring.echoapp;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spring.LambdaSpringApplicationInitializer;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.context.ConfigurableWebApplicationContext;

@Configuration
@ComponentScan("com.amazonaws.serverless.proxy.spring.echoapp")
@PropertySource("classpath:application.properties")
public class EchoSpringAppConfig {

    @Autowired
    private ConfigurableWebApplicationContext applicationContext;

    @Bean
    public SpringLambdaContainerHandler springLambdaContainerHandler() throws ContainerInitializationException {
        SpringLambdaContainerHandler handler = SpringLambdaContainerHandler.getAwsProxyHandler(applicationContext);
        handler.setRefreshContext(false);
        return handler;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public MockLambdaContext lambdaContext() {
        return new MockLambdaContext();
    }

}
