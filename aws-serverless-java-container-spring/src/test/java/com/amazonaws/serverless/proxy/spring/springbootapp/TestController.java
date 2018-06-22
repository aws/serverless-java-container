package com.amazonaws.serverless.proxy.spring.springbootapp;


import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;


@RestController
@EnableWebSecurity
public class TestController extends WebSecurityConfigurerAdapter {
    public static final String TEST_VALUE = "test";

    // workaround to address the most annoying issue in the world: https://blog.georgovassilis.com/2015/10/29/spring-mvc-rest-controller-says-406-when-emails-are-part-url-path/
    @Configuration
    public static class CustomConfig extends WebMvcConfigurerAdapter {
        @Override
        public void configurePathMatch(PathMatchConfigurer configurer) {
            configurer.setUseSuffixPatternMatch(false);
        }

        @Override
        public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
            configurer.favorPathExtension(false);
        }
    }

    @RequestMapping(path = "/test", method = { RequestMethod.GET })
    public SingleValueModel testGet() {
        SingleValueModel value = new SingleValueModel();
        value.setValue(TEST_VALUE);
        return value;
    }

    @RequestMapping(path = "/test/{domain}", method = { RequestMethod.GET})
    public SingleValueModel testDomainInPath(@PathVariable("domain") String domainName) {
        SingleValueModel value = new SingleValueModel();
        value.setValue(domainName);
        return value;
    }

    @RequestMapping(path = "/test/query-string", method = { RequestMethod.GET })
    public SingleValueModel testQueryStringList(@RequestParam("list") List<String> qsValues) {
        assert qsValues != null;
        System.out.println("QUERY_STRING_VALUES: " + qsValues);
        SingleValueModel value = new SingleValueModel();
        value.setValue(qsValues.size() + "");
        return value;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement().disable();
    }
}
