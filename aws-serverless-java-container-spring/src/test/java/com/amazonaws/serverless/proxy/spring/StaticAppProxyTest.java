package com.amazonaws.serverless.proxy.spring;


import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spring.staticapp.LambdaHandler;

import com.amazonaws.services.lambda.runtime.events.AwsProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayProxyRequestEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import jakarta.ws.rs.core.MediaType;

import static com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletRequest.getFirst;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class StaticAppProxyTest {

    private LambdaHandler lambdaHandler = new LambdaHandler();

    @Test
    void staticPage() {
        APIGatewayProxyRequestEvent req = new AwsProxyRequestBuilder("/sample/page", "GET").build();
        // we temporarily allow the container to read from any path
        LambdaContainerHandler.getContainerConfig().addValidFilePath("/");
        AwsProxyResponseEvent resp = lambdaHandler.handleRequest(req, new MockLambdaContext());

        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.getBody().startsWith("<!DOCTYPE html>"));
        assertTrue(resp.getMultiValueHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
        assertEquals(MediaType.TEXT_HTML, getFirst(resp.getMultiValueHeaders(), HttpHeaders.CONTENT_TYPE));
    }
}
