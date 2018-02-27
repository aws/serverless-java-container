package com.amazonaws.serverless.proxy.spring.echoapp;


import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;


@ControllerAdvice
@RequestMapping(produces = "application/json")
@ResponseBody
public class RestControllerAdvice {
    public static final String ERROR_MESSAGE = "UnhadledPath";

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<SingleValueModel> unhandledPath(final NoHandlerFoundException e) {
        SingleValueModel model = new SingleValueModel();
        model.setValue(ERROR_MESSAGE);
        return new ResponseEntity<SingleValueModel>(model, HttpStatus.NOT_FOUND);
    }
}