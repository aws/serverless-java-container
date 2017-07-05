package com.amazonaws.serverless.proxy.spark;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;

import org.junit.Test;

import static org.junit.Assert.*;
import static spark.Spark.get;


public class HelloWorldSparkTest {
    private static final String CUSTOM_HEADER_KEY = "X-Custom-Header";
    private static final String CUSTOM_HEADER_VALUE = "My Header Value";
    private static final String BODY_TEXT_RESPONSE = "Hello World";

    private static SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    @Test
    public void basicServer_initialize() {
        try {
            handler = SparkLambdaContainerHandler.getAwsProxyHandler();

            configureRoutes();

        } catch (RuntimeException | ContainerInitializationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void basicServer_handleRequest_emptyFilters() {
        AwsProxyRequest req = new AwsProxyRequestBuilder().method("GET").path("/hello").build();
        AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey(CUSTOM_HEADER_KEY));
        assertEquals(CUSTOM_HEADER_VALUE, response.getHeaders().get(CUSTOM_HEADER_KEY));
        assertEquals(BODY_TEXT_RESPONSE, response.getBody());
    }

    private void configureRoutes() {
        get("/hello", (req, res) -> {
            res.status(200);
            res.header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE);
            return BODY_TEXT_RESPONSE;
        });
    }
}
