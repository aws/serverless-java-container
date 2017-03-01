package com.amazonaws.serverless.proxy.spring.echoapp;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Profile("override")
@PropertySource("classpath:/application-override.properties")
public class ProfileOverrideConfig {
}