package com.amazonaws.serverless.proxy.spring.servletapp;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

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

    @RequestMapping(path="/message", method = RequestMethod.POST)
    public String returnMessage(@RequestBody MessageData data) {
        if (data == null) {
            throw new RuntimeException("No message data");
        }
        return data.getMessage();
    }

    @RequestMapping(path="/echo/{message}", method=RequestMethod.GET)
    public String returnPathMessage(@PathVariable(value="message") String message) {
        return message;
    }
}
