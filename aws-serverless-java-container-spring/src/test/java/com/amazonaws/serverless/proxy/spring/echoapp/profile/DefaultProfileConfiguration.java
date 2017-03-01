package com.amazonaws.serverless.proxy.spring.echoapp.profile;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultProfileConfiguration {
    @Bean
    public String beanInjectedValue() {
        return "default-profile-from-bean";
    }
}