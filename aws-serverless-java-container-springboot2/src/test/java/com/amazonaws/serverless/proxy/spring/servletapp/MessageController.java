package com.amazonaws.serverless.proxy.spring.servletapp;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
public class MessageController {
    public static final String HELLO_MESSAGE = "Hello";
    public static final String VALID_MESSAGE = "VALID";
    public static final String UTF8_RESPONSE = "öüäß фрыцшщ";
    public static final String EX_MESSAGE = "404 exception message";

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

    @GetMapping(value = "/content-type/utf8", produces = "text/plain")
    public ResponseEntity<String> getUtf8String() {
        return ResponseEntity.ok(UTF8_RESPONSE);
    }

    @GetMapping(value = "/content-type/jsonutf8", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getUtf8Json() {
        Map<String, String> resp = new HashMap<String, String>();
        resp.put("s", UTF8_RESPONSE);
        return ResponseEntity.ok(resp);
    }

    @GetMapping(value = "/ex/customstatus")
    public String throw404Exception() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, EX_MESSAGE);
    }
}
