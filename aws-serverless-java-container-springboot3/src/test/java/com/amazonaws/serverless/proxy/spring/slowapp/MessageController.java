package com.amazonaws.serverless.proxy.spring.slowapp;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {
    public static final String HELLO_MESSAGE = "Hello";

    @RequestMapping(path="/hello", method= RequestMethod.GET)
    public String hello() {
        return HELLO_MESSAGE;
    }
}
