package com.amazonaws.serverless.proxy.spring.securityapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFluxSecurity
@EnableWebFlux
@Import(SecurityConfig.class)
public class SecurityApplication {
}
