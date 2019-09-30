package com.amazonaws.serverless.proxy.spring.webfluxapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class
})
public class WebFluxTestApplication {
}
