package com.amazonaws.serverless.proxy.spring.echoapp;

import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.context.annotation.PropertySource;


@Configuration
@ComponentScan("com.amazonaws.serverless.proxy.spring.echoapp")
@PropertySource("classpath:application.properties")
public class EchoSpringAppConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public MockLambdaContext lambdaContext() {
        return new MockLambdaContext();
    }

    @Bean
    public javax.validation.Validator localValidatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }
}
