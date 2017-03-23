package com.amazonaws.serverless.proxy.spring.echoapp;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.ServletContext;

@RestController
@EnableWebMvc
@RequestMapping("/context")
public class ContextResource implements ServletContextAware {
    private ServletContext context;

    @RequestMapping(path = "/echo", method= RequestMethod.GET)
    public ResponseEntity<String> getContext() {
        return new ResponseEntity<String>(this.context.getServerInfo(), HttpStatus.OK);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        context = servletContext;
    }
}
