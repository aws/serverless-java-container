package com.amazonaws.serverless.proxy.spring.springbootapp;


import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@EnableWebSecurity
public class TestController extends WebSecurityConfigurerAdapter{
    public static final String TEST_VALUE = "test";

    @RequestMapping(path = "/test", method = { RequestMethod.GET })
    public SingleValueModel testGet() {
        SingleValueModel value = new SingleValueModel();
        value.setValue(TEST_VALUE);
        return value;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement().disable();
    }
}
