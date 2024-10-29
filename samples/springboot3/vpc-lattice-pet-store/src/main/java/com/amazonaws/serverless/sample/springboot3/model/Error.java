package com.amazonaws.serverless.sample.springboot3.model;

public class Error {
    private String message;

    public Error(String errorMessage) {
        message = errorMessage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}