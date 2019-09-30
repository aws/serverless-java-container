package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.slowapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.slowapp.MessageController;
import com.amazonaws.serverless.proxy.spring.slowapp.SlowTestApplication;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SlowAppTest {

    @Test
    public void slowAppInit_continuesInBackgroundThread_returnsCorrect() {
        LambdaHandler slowApp = new LambdaHandler();
        System.out.println("Start time: " + slowApp.getConstructorTime());
        assertTrue(slowApp.getConstructorTime() < 10_000);
        AwsProxyRequest req = new AwsProxyRequestBuilder("/hello", "GET").build();
        long startRequestTime = Instant.now().toEpochMilli();
        AwsProxyResponse resp = slowApp.handleRequest(req, new MockLambdaContext());
        long endRequestTime = Instant.now().toEpochMilli();
        assertTrue(endRequestTime - startRequestTime > SlowTestApplication.SlowDownInit.INIT_SLEEP_TIME_MS - 10_000);
        assertEquals(200, resp.getStatusCode());
        Assert.assertEquals(MessageController.HELLO_MESSAGE, resp.getBody());
    }
}
