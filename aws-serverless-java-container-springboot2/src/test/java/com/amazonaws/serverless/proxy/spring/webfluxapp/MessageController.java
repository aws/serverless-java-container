package com.amazonaws.serverless.proxy.spring.webfluxapp;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class MessageController {
    public static final String MESSAGE = "Hello";

    @RequestMapping(path="/single", method= RequestMethod.GET, produces = {"text/plain"})
    Flux<String> singleMessage(){
        return Flux.just(
                MESSAGE
        );
    }

    @RequestMapping(path="/double", method= RequestMethod.GET, produces={"text/plain"})
    Flux<String> doubleMessage(){
        return Flux.just(
                MESSAGE,
                MESSAGE
        );
    }
}