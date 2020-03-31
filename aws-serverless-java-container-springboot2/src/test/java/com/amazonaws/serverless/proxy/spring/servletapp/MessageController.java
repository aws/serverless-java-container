package com.amazonaws.serverless.proxy.spring.servletapp;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
public class MessageController {
    public static final String HELLO_MESSAGE = "Hello";
    public static final String VALID_MESSAGE = "VALID";

    @RequestMapping(path="/hello", method=RequestMethod.GET, produces = {"text/plain"})
    public String hello() {
        return HELLO_MESSAGE;
    }

    @RequestMapping(path="/validate", method=RequestMethod.POST, produces = {"text/plain"})
    public ResponseEntity<String> validateBody(@RequestBody @Valid UserData userData, Errors errors) {
        if (errors != null && errors.hasErrors()) {
            return ResponseEntity.badRequest().body(errors.getErrorCount() + "");
        }
        return ResponseEntity.ok(VALID_MESSAGE);
    }
}
