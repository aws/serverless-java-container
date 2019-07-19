package com.amazonaws.serverless.proxy.spring.springbootapp;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;


@SpringBootApplication
@ComponentScan(basePackages = "com.amazonaws.serverless.proxy.spring.springbootapp")
@PropertySource("classpath:boot-application.properties")
public class TestApplication extends SpringBootServletInitializer {
}
