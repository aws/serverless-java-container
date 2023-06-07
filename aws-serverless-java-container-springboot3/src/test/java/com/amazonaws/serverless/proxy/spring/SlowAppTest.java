package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spring.slowapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.slowapp.MessageController;
import com.amazonaws.serverless.proxy.spring.slowapp.SlowTestApplication;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SlowAppTest {

    @Test
    void slowAppInit_continuesInBackgroundThread_returnsCorrect() {
        LambdaHandler slowApp = new LambdaHandler();
        System.out.println("Start time: " + slowApp.getConstructorTime());
        assertTrue(slowApp.getConstructorTime() < 10_000);
        APIGatewayProxyRequestEvent req = new AwsProxyRequestBuilder("/hello", "GET").build();
        long startRequestTime = Instant.now().toEpochMilli();
        APIGatewayProxyResponseEvent resp = slowApp.handleRequest(req, new MockLambdaContext());
        long endRequestTime = Instant.now().toEpochMilli();
        assertTrue(endRequestTime - startRequestTime > SlowTestApplication.SlowDownInit.INIT_SLEEP_TIME_MS - 10_000);
        assertEquals(200, resp.getStatusCode());
        assertEquals(MessageController.HELLO_MESSAGE, resp.getBody());
    }
}
