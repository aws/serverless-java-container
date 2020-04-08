package com.amazonaws.serverless.proxy.spring.servletapp;

public class MessageData {
    private String message;

    public MessageData() {
    }

    public MessageData(String m) {
        setMessage(m);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
