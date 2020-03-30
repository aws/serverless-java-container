package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.springbootslowapp.SBLambdaHandler;
import com.amazonaws.serverless.proxy.spring.springslowapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.springslowapp.MessageController;
import com.amazonaws.serverless.proxy.spring.springslowapp.SlowAppConfig;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.SpringVersion;

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

    @Test
    public void springBootSlowApp_continuesInBackgroundThread_returnsCorrect() {
        // We skip the tests if we are running against Spring 5.2.x - SpringBoot 1.5 is deprecated and no longer
        // breaking changes in the latest Spring releases have not been supported in it.
        // TODO: Update the check to verify any Spring version above 5.2
        assumeFalse(Objects.requireNonNull(SpringVersion.getVersion()).startsWith("5.2"));

        SBLambdaHandler slowApp = null;
        try {
            slowApp = new SBLambdaHandler();
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
