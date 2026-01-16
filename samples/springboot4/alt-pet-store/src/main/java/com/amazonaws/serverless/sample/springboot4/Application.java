package com.amazonaws.serverless.sample.springboot4;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.amazonaws.serverless.sample.springboot4.controller.PetsController;
import com.amazonaws.serverless.sample.springboot4.filter.CognitoIdentityFilter;

import jakarta.servlet.Filter;


@SpringBootApplication
@Import({ PetsController.class })
public class Application {

    // silence console logging
    @Value("${logging.level.root:OFF}")
    String message = "";

    /*
     * Create required HandlerMapping, to avoid several default HandlerMapping instances being created
     */
    @Bean
    public HandlerMapping handlerMapping() {
        return new RequestMappingHandlerMapping();
    }

    /*
     * Create required HandlerAdapter, to avoid several default HandlerAdapter instances being created
     */
    @Bean
    public HandlerAdapter handlerAdapter() {
        return new RequestMappingHandlerAdapter();
    }

    @Bean("CognitoIdentityFilter")
    public Filter cognitoFilter() {
        return new CognitoIdentityFilter();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}