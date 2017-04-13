package com.amazonaws.serverless.proxy.spring.echoapp;

import com.amazonaws.serverless.proxy.spring.echoapp.model.ValidatedUserModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.ServletContext;
import javax.validation.Valid;
import javax.ws.rs.core.MediaType;

@RestController
@EnableWebMvc
@RequestMapping("/context")
public class ContextResource implements ServletContextAware {
    private ServletContext context;

    @RequestMapping(path = "/echo", method= RequestMethod.GET)
    public ResponseEntity<String> getContext() {
        return new ResponseEntity<String>(this.context.getServerInfo(), HttpStatus.OK);
    }

    @RequestMapping(path = "/user", method=RequestMethod.POST, consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<ValidatedUserModel> createUser(@Valid @RequestBody ValidatedUserModel newUser, BindingResult results) {

        if (results.hasErrors()) {
            System.out.println("Has errors");
            return new ResponseEntity<ValidatedUserModel>(newUser, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<ValidatedUserModel>(newUser, HttpStatus.OK);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        context = servletContext;
    }
}
