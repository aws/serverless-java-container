package com.amazonaws.serverless.proxy.spring.sbsecurityapp;


import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.List;


@RestController
public class TestController {
    public static final String ACCESS_DENIED = "AccessDenied";

    @RequestMapping(path = "/user", method = { RequestMethod.GET })
    public SingleValueModel testGet() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SingleValueModel value = new SingleValueModel();

        if (principal instanceof UserDetails) {
            String username = ((UserDetails)principal).getUsername();
            value.setValue(username);
        } else {
            value.setValue(null);
        }
        return value;
    }

    @RequestMapping(path = "/access-denied", method = { RequestMethod.GET })
    public SingleValueModel accessDenied() {
        SingleValueModel model = new SingleValueModel();
        model.setValue(ACCESS_DENIED);
        return model;
    }


}
