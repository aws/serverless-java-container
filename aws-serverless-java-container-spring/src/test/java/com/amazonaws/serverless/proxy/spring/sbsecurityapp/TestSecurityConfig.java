package com.amazonaws.serverless.proxy.spring.sbsecurityapp;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
public class TestSecurityConfig extends WebSecurityConfigurerAdapter {
    public static final String USERNAME = "test";
    public static final String NO_ADMIN_USERNAME = "test2";
    public static final String PASSWORD = "123";
    public static BCryptPasswordEncoder pEncoder = new BCryptPasswordEncoder();
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser(USERNAME).password(pEncoder.encode(PASSWORD)).roles("ADMIN")
                .and().withUser(NO_ADMIN_USERNAME).password(pEncoder.encode(PASSWORD)).roles("USER")
                .and().passwordEncoder(pEncoder);

    }

    @Override
    public void configure(WebSecurity web) throws Exception {
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.httpBasic();
        http
                .authorizeRequests()
                .antMatchers("/user").hasRole("ADMIN")
                .and().exceptionHandling().accessDeniedPage("/access-denied");
    }
}
