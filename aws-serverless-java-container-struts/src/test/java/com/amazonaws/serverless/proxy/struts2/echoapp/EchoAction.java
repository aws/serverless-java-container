package com.amazonaws.serverless.proxy.struts2.echoapp;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.commons.io.IOUtils;
import org.apache.struts2.ServletActionContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class EchoAction extends ActionSupport {

    private String message;

    public String execute() throws IOException {
        HttpServletRequest request = ServletActionContext.getRequest();

        if (message == null && requestHasBody(request)) {
            message = IOUtils.toString(request.getReader());
        }

        return SUCCESS;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCustomHeader(boolean customHeader) {
        if (customHeader) {
            HttpServletResponse response = ServletActionContext.getResponse();
            response.setHeader("XX", "FOO");
        }
    }


    public void setContentType(boolean contentType) {
        if (contentType) {
            HttpServletResponse response = ServletActionContext.getResponse();
            response.setContentType("application/json");
        }
    }

    private boolean requestHasBody(HttpServletRequest request) throws IOException {
        return ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) && request.getReader() != null;
    }

}
