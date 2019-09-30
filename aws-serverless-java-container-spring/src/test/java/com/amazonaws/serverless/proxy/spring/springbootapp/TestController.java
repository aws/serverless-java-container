package com.amazonaws.serverless.proxy.spring.springbootapp;


import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@EnableWebSecurity
public class TestController extends WebSecurityConfigurerAdapter {
    public static final String TEST_VALUE = "test";
    public static final String UTF8_TEST_STRING = "health心跳测试完成。可正常使用";
    public static final String CUSTOM_HEADER_NAME = "X-Custom-Header";
    public static final String CUSTOM_QS_NAME = "qs";

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

    @RequestMapping(path = "/missing-params", method = {RequestMethod.GET})
    public Map<String, Boolean> testInvalidParameters(@RequestHeader(CUSTOM_HEADER_NAME) String h1, @RequestParam(CUSTOM_QS_NAME) String qsValue) {
        Map<String, Boolean> output = new HashMap<>();
        output.put("header", h1 == null);
        output.put("queryString", qsValue == null);
        return output;
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
        SingleValueModel value = new SingleValueModel();
        value.setValue(qsValues.size() + "");
        return value;
    }

    @RequestMapping(value="/test/utf8",method=RequestMethod.GET)
    public Object testUtf8(String name, HttpServletResponse response){
        SingleValueModel model = new SingleValueModel();
        model.setValue(UTF8_TEST_STRING);
        return model;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement().disable();
    }
}
