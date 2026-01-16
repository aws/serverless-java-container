package com.amazonaws.serverless.proxy.spring.servletapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@Import(MessageController.class)
@RestController
public class ServletApplication {
	
	@RequestMapping(path = "/foo/{gender}/list/{age}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String complexRequest(
            @PathVariable("gender") String gender,
            @PathVariable("age") String age,
            @RequestParam("name") String name
    ) {
		return gender + "/" + age + "/" + name;
	}
}
