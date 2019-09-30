package com.amazonaws.serverless.proxy.spring.securityapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication()
public class ServletApplication {
}
