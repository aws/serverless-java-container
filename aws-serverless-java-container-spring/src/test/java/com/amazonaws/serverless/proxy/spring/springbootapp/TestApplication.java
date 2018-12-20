package com.amazonaws.serverless.proxy.spring.springbootapp;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = "com.amazonaws.serverless.proxy.spring.springbootapp")
public class TestApplication extends SpringBootServletInitializer {
}
