package com.amazonaws.serverless.proxy.spring.springapp;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@RestController
@EnableWebMvc
public class MessageController {
    public static final String HELLO_MESSAGE = "Hello";

    @RequestMapping(path="/hello", method= RequestMethod.GET)
    public String hello() {
        return HELLO_MESSAGE;
    }

    @RequestMapping(path="/async", method= RequestMethod.GET)
    public DeferredResult<String> asyncHello() {
        DeferredResult<String> result = new DeferredResult<>();
        result.setResult(HELLO_MESSAGE);
        return result;
    }
}
