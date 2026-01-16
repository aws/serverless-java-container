package com.amazonaws.serverless.proxy.spring.securityapp;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
public class MessageController {
    public static final String HELLO_MESSAGE = "Hello";

    @RequestMapping(path="/hello", method=RequestMethod.GET, produces = {"text/plain"})
    public Mono<String> hello() {
        return Mono.just(HELLO_MESSAGE);
    }
}
