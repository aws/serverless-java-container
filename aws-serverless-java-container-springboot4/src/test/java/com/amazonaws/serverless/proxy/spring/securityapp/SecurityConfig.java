package com.amazonaws.serverless.proxy.spring.securityapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig
{
    public static final String USERNAME = "admin";
    public static final String PASSWORD = "{noop}password";
    private static BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Bean
    public SecurityWebFilterChain securitygWebFilterChain(
            ServerHttpSecurity http) {
        return http.authorizeExchange()
                .anyExchange().authenticated().and().csrf().disable()
                .httpBasic()
                .and().build();
    }

    @Bean
    public static BCryptPasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User
                .withUsername(USERNAME)
                .password(passwordEncoder.encode(PASSWORD))
                .roles("USER")
                .build();
        return new MapReactiveUserDetailsService(user);
    }
}