package com.amazonaws.serverless.proxy.spring.webfluxapp;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
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

    @RequestMapping(path="/message", method = RequestMethod.POST, produces={"text/plain"}, consumes = {"application/json"})
    public Flux<String> returnMessage(@RequestBody MessageData data) {
        if (data == null) {
            throw new RuntimeException("No message data");
        }
        return Flux.just(data.getMessage());
    }
}