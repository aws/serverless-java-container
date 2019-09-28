package com.amazonaws.serverless.proxy.spring.springslowapp;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@RestController
@EnableWebMvc
public class MessageController {
    public static final String HELLO_MESSAGE = "Hello";

    @RequestMapping(path="/hello", method= RequestMethod.GET)
    public String hello() {
        return HELLO_MESSAGE;
    }
}
