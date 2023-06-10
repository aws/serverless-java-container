package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.InvalidResponseObjectException;
import com.amazonaws.serverless.proxy.ResponseWriter;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import jakarta.ws.rs.core.Response;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AwsAlbHttpServletResponseWriter extends ResponseWriter<AwsHttpServletResponse, ApplicationLoadBalancerResponseEvent> {

    private boolean writeSingleValueHeaders;

    public AwsAlbHttpServletResponseWriter() {
        this(false);
    }

    public AwsAlbHttpServletResponseWriter(boolean singleValueHeaders) {
        writeSingleValueHeaders = singleValueHeaders;
    }
    @Override
    public ApplicationLoadBalancerResponseEvent writeResponse(AwsHttpServletResponse containerResponse, Context lambdaContext) throws InvalidResponseObjectException {
        Timer.start("SERVLET_RESPONSE_WRITE");
        ApplicationLoadBalancerResponseEvent awsProxyResponse = new ApplicationLoadBalancerResponseEvent();
        if (containerResponse.getAwsResponseBodyString() != null) {
            String responseString;

            if (!isBinary(containerResponse.getContentType()) && isValidUtf8(containerResponse.getAwsResponseBodyBytes())) {
                responseString = containerResponse.getAwsResponseBodyString();
            } else {
                responseString = Base64.getEncoder().encodeToString(containerResponse.getAwsResponseBodyBytes());
                awsProxyResponse.setIsBase64Encoded(true);
            }

            awsProxyResponse.setBody(responseString);
        }
        awsProxyResponse.setMultiValueHeaders(containerResponse.getAwsResponseHeaders());
        if (writeSingleValueHeaders) {
            awsProxyResponse.setHeaders(toSingleValueHeaders(containerResponse.getAwsResponseHeaders()));
        }

        awsProxyResponse.setStatusCode(containerResponse.getStatus());

        Response.Status responseStatus = Response.Status.fromStatusCode(containerResponse.getStatus());

        if (containerResponse.getAwsAlbRequest() != null && responseStatus != null) {
            awsProxyResponse.setStatusDescription(containerResponse.getStatus() + " " + responseStatus.getReasonPhrase());
        }

        Timer.stop("SERVLET_RESPONSE_WRITE");
        return awsProxyResponse;
    }

    private Map<String, String> toSingleValueHeaders(Headers h) {
        Map<String, String> out = new HashMap<>();
        if (h == null || h.isEmpty()) {
            return out;
        }
        for (String k : h.keySet()) {
            out.put(k, h.getFirst(k));
        }
        return out;
    }

    private boolean isBinary(String contentType) {
        if(contentType != null) {
            int semidx = contentType.indexOf(';');
            if(semidx >= 0) {
                return LambdaContainerHandler.getContainerConfig().isBinaryContentType(contentType.substring(0, semidx));
            }
            else {
                return LambdaContainerHandler.getContainerConfig().isBinaryContentType(contentType);
            }
        }
        return false;
    }
}
