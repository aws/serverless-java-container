package com.amazonaws.serverless.proxy.spring.echoapp.profile;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("override")
public class OverrideProfileConfiguration {
    @Bean
    public String beanInjectedValue() {
        return "override-profile-from-bean";
    }
}