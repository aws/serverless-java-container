package com.amazonaws.serverless.proxy.struts2.echoapp;

import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


public class EchoRequestInfoAction extends ActionSupport {

    private String mode = "principal";
    private Object result = null;

    public String execute() {

        HttpServletRequest request = ServletActionContext.getRequest();
        AwsProxyRequestContext awsProxyRequestContext =
                (AwsProxyRequestContext) request
                        .getAttribute(RequestReader.API_GATEWAY_CONTEXT_PROPERTY);

        switch (mode) {
            case "principal":
                result = awsProxyRequestContext.getAuthorizer().getPrincipalId();
                break;
            case "scheme":
                result = request.getScheme();
                break;
            case "content-type":
                if (request.getContentType().contains("application/octet-stream")) {
                    ServletActionContext.getResponse().setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                }
                result = request.getContentType();
                break;
            case "not-allowed":
                ServletActionContext.getResponse().setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                break;
            case "custom-status-code":
                ServletActionContext.getResponse().setStatus(HttpServletResponse.SC_CREATED);
                break;
            case "not-implemented":
                ServletActionContext.getResponse().setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
                break;
            case "headers":
                Map<String, String> headers = new HashMap<>();

                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    headers.put(headerName, request.getHeader(headerName));
                }

                result = headers;
                break;
            case "query-string":
                Map<String, String> params = new HashMap<>();

                Enumeration<String> parameterNames = request.getParameterNames();
                while (parameterNames.hasMoreElements()) {
                    String parameterName = parameterNames.nextElement();
                    params.put(parameterName, request.getParameter(parameterName));
                }

                result = params;
                break;
            default:
                throw new IllegalArgumentException("Invalid mode requested: " + mode);
        }

        return SUCCESS;
    }

    public Object getResult() {
        return result;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
