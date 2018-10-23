package com.amazonaws.serverless.proxy.spring;


import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spring.staticapp.LambdaHandler;

import org.junit.Test;
import org.springframework.http.HttpHeaders;

import javax.ws.rs.core.MediaType;

import static org.junit.Assert.*;


public class StaticAppProxyTest {

    private LambdaHandler lambdaHandler = new LambdaHandler();

    @Test
    public void staticPage() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/sample/page", "GET").build();
        // we temporarily allow the container to read from any path
        LambdaContainerHandler.getContainerConfig().addValidFilePath("/");
        AwsProxyResponse resp = lambdaHandler.handleRequest(req, new MockLambdaContext());

        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.getBody().startsWith("<!DOCTYPE html>"));
        assertTrue(resp.getMultiValueHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
        assertEquals(MediaType.TEXT_HTML, resp.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }
}
