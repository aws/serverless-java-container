package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.springapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.springapp.MessageController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class AsyncAppTest {

    private static LambdaHandler handler;

    @BeforeAll
    public static void setUp() {
        try {
            handler = new LambdaHandler();
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void springApp_helloRequest_returnsCorrect() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/hello", "GET").build();
        AwsProxyResponse resp = handler.handleRequest(req, new MockLambdaContext());
        assertEquals(200, resp.getStatusCode());
        assertEquals(MessageController.HELLO_MESSAGE, resp.getBody());
    }

    @Test
    void springApp_asyncRequest_returnsCorrect() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/async", "GET").build();
        AwsProxyResponse resp = handler.handleRequest(req, new MockLambdaContext());
        assertEquals(200, resp.getStatusCode());
        assertEquals(MessageController.HELLO_MESSAGE, resp.getBody());
    }

}
