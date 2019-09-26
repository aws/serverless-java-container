package com.amazonaws.serverless.proxy.spring.securityapp;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    public static final String USERNAME = "user";
    public static String PASSWORD = "testPassword";
    public static BCryptPasswordEncoder pEncoder = new BCryptPasswordEncoder();

    @Bean
    public SecurityWebFilterChain securitygWebFilterChain(
            ServerHttpSecurity http) {
        return http.authorizeExchange()
                .anyExchange().authenticated()
                .and().httpBasic()
                .and().build();
    }

    @Bean
    public MapReactiveUserDetailsService reactiveUserDetailsService(SecurityProperties properties, ObjectProvider<PasswordEncoder> passwordEncoder) {
        return new MapReactiveUserDetailsService(getUser());
    }

    private UserDetails getUser() {
        return User.builder().username(USERNAME).password(passwordEncoder().encode(PASSWORD)).authorities("USER").build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return pEncoder;
    }

    /*@Autowired
    public void configureGlobal(AuthenticationManagerBuilder authentication)
            throws Exception
    {
        authentication.inMemoryAuthentication()
                .withUser(USERNAME)
                .password(passwordEncoder().encode(PASSWORD))
                .authorities("ROLE_USER");
        if (userService != null) {
            if (userService.loadUserByUsername("user") != null) {
                System.out.println("Setting password in configureGlobal");
                PASSWORD = userService.loadUserByUsername("user").getPassword().replace("{noop}", "");
            }
        }
    }*/
}
