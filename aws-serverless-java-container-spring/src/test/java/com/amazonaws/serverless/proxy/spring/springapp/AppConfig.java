package com.amazonaws.serverless.proxy.spring.springapp;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({MessageController.class})
public class AppConfig { }
