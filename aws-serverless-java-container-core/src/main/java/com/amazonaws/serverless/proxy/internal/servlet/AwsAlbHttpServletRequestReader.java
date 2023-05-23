package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Simple implementation of the <code>RequestReader</code> interface that receives an <code>ApplicationLoadBalancerRequestEvent</code>
 * object and uses it to initialize a <code>AwsProxyHttpServletRequest</code> object.
 */
public class AwsAlbHttpServletRequestReader extends RequestReader<ApplicationLoadBalancerRequestEvent, HttpServletRequest> {

    static final String INVALID_REQUEST_ERROR = "The incoming event is not a valid request from an Application Load Balancer";

    private ServletContext servletContext;

    //-------------------------------------------------------------
    // Methods - Implementation
    //-------------------------------------------------------------
    public void setServletContext(ServletContext ctx) {
        servletContext = ctx;
    }
    @Override
    public HttpServletRequest readRequest(ApplicationLoadBalancerRequestEvent request, SecurityContext securityContext, Context lambdaContext, ContainerConfig config) throws InvalidRequestEventException {
        // Expect the HTTP method and context to be populated. If they are not, we are handling an
        // unsupported event type.
        if (request.getHttpMethod() == null || request.getHttpMethod().equals("") || request.getRequestContext() == null) {
            throw new InvalidRequestEventException(INVALID_REQUEST_ERROR);
        }

        request.setPath(stripBasePath(request.getPath(), config));
        if (request.getMultiValueHeaders() != null && request.getMultiValueHeaders().get(HttpHeaders.CONTENT_TYPE) != null && request.getMultiValueHeaders().get(HttpHeaders.CONTENT_TYPE).get(0) != null) {   //TODO: check
            String contentType = request.getMultiValueHeaders().get(HttpHeaders.CONTENT_TYPE).get(0);
            // put single as we always expect to have one and only one content type in a request.
            request.getMultiValueHeaders().put(HttpHeaders.CONTENT_TYPE, new ArrayList<>(Arrays.asList(getContentTypeWithCharset(contentType, config))));
        }
        AwsAlbHttpServletRequest servletRequest = new AwsAlbHttpServletRequest(request, lambdaContext, securityContext, config);
        servletRequest.setServletContext(servletContext);
        //servletRequest.setAttribute(API_GATEWAY_CONTEXT_PROPERTY, request.getRequestContext());
        //servletRequest.setAttribute(API_GATEWAY_STAGE_VARS_PROPERTY, request.getStageVariables());
        //servletRequest.setAttribute(API_GATEWAY_EVENT_PROPERTY, request);
        servletRequest.setAttribute(ALB_CONTEXT_PROPERTY, request.getRequestContext());
        servletRequest.setAttribute(LAMBDA_CONTEXT_PROPERTY, lambdaContext);
        servletRequest.setAttribute(JAX_SECURITY_CONTEXT_PROPERTY, securityContext);

        return servletRequest;
    }

    @Override
    protected Class<? extends ApplicationLoadBalancerRequestEvent> getRequestClass() {
        return ApplicationLoadBalancerRequestEvent.class;
    }

    private String getContentTypeWithCharset(String headerValue, ContainerConfig config) {
        if (headerValue == null || "".equals(headerValue.trim())) {
            return headerValue;
        }

        if (headerValue.contains("charset=")) {
            return headerValue;
        }

        String newValue = headerValue;
        if (!headerValue.trim().endsWith(";")) {
            newValue += "; ";
        }

        newValue += "charset=" + config.getDefaultContentCharset();
        return newValue;
    }
}
