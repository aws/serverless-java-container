package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.springslowapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.springslowapp.MessageController;
import com.amazonaws.serverless.proxy.spring.springslowapp.SlowAppConfig;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class SlowAppTest {

    @Test
    public void springSlowApp_continuesInBackgroundThread_returnsCorrect() {
        LambdaHandler slowApp = null;
        try {
            slowApp = new LambdaHandler();
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
            fail("Exception during initialization");
        }
        System.out.println("Start time: " + slowApp.getConstructorTime());
        assertTrue(slowApp.getConstructorTime() < 10_000);
        AwsProxyRequest req = new AwsProxyRequestBuilder("/hello", "GET").build();
        long startRequestTime = Instant.now().toEpochMilli();
        AwsProxyResponse resp = slowApp.handleRequest(req, new MockLambdaContext());
        long endRequestTime = Instant.now().toEpochMilli();
        assertTrue(endRequestTime - startRequestTime > SlowAppConfig.SlowDownInit.INIT_SLEEP_TIME_MS - 10_000);
        assertEquals(200, resp.getStatusCode());
        Assert.assertEquals(MessageController.HELLO_MESSAGE, resp.getBody());
    }

}
